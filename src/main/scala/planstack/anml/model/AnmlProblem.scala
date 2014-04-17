package planstack.anml.model

import collection.JavaConversions._
import planstack.anml.{ANMLException, parser}

import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList
import planstack.anml.parser.{ParseResult, FuncExpr, VarExpr}
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import planstack.anml.model.concrete._
import planstack.anml.model.concrete.statements.TemporalStatement
import planstack.anml.model.abs.{AbstractTemporalConstraint, AbstractTemporalStatement, AbstractActionRef, AbstractAction}
import scala.Some

/** Description of an ANML problem.
  *
  * An ANML problem comes with three time points: start (global start of the world the problem takes place in),
  * end (global end of the world) and earliestExecution which represent the earliest time at which an action can be executed.
  * Temporal intervals (such as actions, statements, ...) are placed relatively to the start and end time points using
  * [[planstack.anml.model.concrete.TemporalConstraint]].
  * The earliestExecution time point will not have any explicit constraints linked to it (we consider this choice is
  * plannner-dependent and is not explicitly defined in ANML).
  *
  * Components include:
  *
  *  - an [[planstack.anml.model.InstanceManager]] that keeps track of all types and instances declared
  * in the problem.
  *
  *  - a [[planstack.anml.model.FunctionManager]] that records of all functions declared in the ANML problem.
  *
  *  - a [[planstack.anml.model.Context]] that map local variables and references that appear in the problem
  *  scope to global ones.
  *
  *
  * Further more an ANML problem consists of a list of [[planstack.anml.model.concrete.StateModifier]]
  * representing everything that must be added to plan solving this problem.
  * A first one is added in the constructor containing predefined instances of the problem (such as true and false).
  * Every time an `addAnml(...)` method it called, the problem's components are updated accordingly and a new
  * [[planstack.anml.model.concrete.StateModifier]] is added to represent the changes in the problems.
  */
class AnmlProblem extends TemporalInterval {

  /**
   * A time-point representing the earliest possible execution of an action.
   * Note that no explicit constraint including this timepoint is specified in the the ANML problem, it
   * is provided here mainly for consistency in the planner implementation. It is the responsability of the
   * planner to enforce the ordering between this time-point and the actions in the plan.
   */
  val earliestExecution : TPRef = new TPRef()

  /**
   * An InstanceManager that keeps track of all types and instances of the problem.
   */
  val instances = new InstanceManager

  /**
   * A [[planstack.anml.model.FunctionManager]] that keeps track of all functions (ie. definition of state variables)
   * in the problem.
   */
  val functions = new FunctionManager

  /** Context that all variables and and action that appear in the problem scope.
    * Those typically contain instances of the problem and predefined ANML literals (such as true, false, ...)
    */
  val context = new Context(None)
  context.setInterval(this)

  /** All abstract actions appearing in the problem */
  val abstractActions = new java.util.LinkedList[AbstractAction]()

  /**
   * All [[planstack.anml.model.concrete.StateModifier]] that need to be applied to a state for it to reprent this problem.
   * There is one modifier encoding the default definitions of an ANML problem (such as the instances true and false of type
   * boolean). One modifier is added by update of the problem (as a result of the invocation of `addAnml(...)`.
   */
  val modifiers = new java.util.LinkedList[StateModifier]()

  // create an initial modifier containing the predefined instances (true and false)
  {
    val originalSM = new BaseStateModifier(this)

    // add predifined isntance to context and to StateModifier
    for((name, tipe) <- instances.instances) {
      originalSM.instances += name
      context.addVar(new LVarRef(name), tipe, instances.referenceOf(name))
    }

    modifiers += originalSM
  }

  /**
   * Retrieves the abstract action with the given name.
   * @param name Name of the action to lookup.
   * @return The corresponding AbstractAction.
   */
  def getAction(name:String) = abstractActions.find(_.name == name) match {
    case Some(act) => act
    case None => throw new ANMLException("No action named "+name)
  }

  /**
   * Integrates new ANML blocks into the problem. If any updates need to be made to a search state as a consequence,
   * those are encoded as a StateModifier and added to `modifiers`
   * @param anml Output of the ANML parser for the ANML block.
   */
  def addAnml(anml:ParseResult) = addAnmlBlocks(anml.blocks)

  /**
   * Integrates new ANML blocks into the problem. If any updates need to be made to a search state as a consequence,
   * those are encoded as a StateModifier and added to `modifiers`
   * @param blocks A sequence of ANML blocks.
   */
  def addAnmlBlocks(blocks:Seq[parser.AnmlBlock]) {

    // modifier that containing all alterations to be made to a plan as a consequence of this ANML block
    val modifier = new BaseStateModifier(this)

    // add all type declarations to the instance manager.
    blocks.filter(_.isInstanceOf[parser.Type]).map(_.asInstanceOf[parser.Type]) foreach(typeDecl => {
      instances.addType(typeDecl.name, typeDecl.parent)
    })

    // add all instance declaration to the instance manager and to the modifier
    blocks.filter(_.isInstanceOf[parser.Instance]).map(_.asInstanceOf[parser.Instance]) foreach(instanceDecl => {
      instances.addInstance(instanceDecl.name, instanceDecl.tipe)
      modifier.instances += instanceDecl.name
      // all instances are added to the context
      context.addVar(new LVarRef(instanceDecl.name), instanceDecl.tipe, instances.referenceOf(instanceDecl.name))
    })

    // add all functions to the function manager
    blocks.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]) foreach(funcDecl => {
      assert(!funcDecl.name.contains("."), "Declaring function "+funcDecl+" is not supported. If you wanted to " +
        "declared a function linked to type, you should do so in the type itself.") // TODO: should be easy to support
      functions.addFunction(funcDecl)
    })

    // find all methods declared inside a type and them to functions and to the type.
    blocks.filter(_.isInstanceOf[parser.Type]).map(_.asInstanceOf[parser.Type]) foreach(typeDecl => {
      typeDecl.content.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]).foreach(scopedFunction => {
        functions.addScopedFunction(typeDecl.name, scopedFunction)
        instances.addMethodToType(typeDecl.name, scopedFunction.name)
      })
    })

    blocks.filter(_.isInstanceOf[parser.TemporalStatement]).map(_.asInstanceOf[parser.TemporalStatement]) foreach(tempStatement => {
      val ts = AbstractTemporalStatement(this, this.context, tempStatement)
      val annotatedStatement = TemporalStatement(this, context, ts)
      modifier.statements += annotatedStatement.statement
      context.addStatement(ts.statement.id, annotatedStatement.statement)
      modifier.temporalConstraints ++= annotatedStatement.getTemporalConstraints
    })

    blocks.filter(_.isInstanceOf[parser.Action]).map(_.asInstanceOf[parser.Action]) foreach(actionDecl => {
      val abs = AbstractAction(actionDecl, this)
      abstractActions += abs

      // if the action is a seed, add it to the modifier to make sure it appears in the initial plan.
      if(abs.name == "Seed" || abs.name == "seed") {
        val localRef = new LActRef()
        val act = Action.getNewStandaloneAction(this, abs)
        context.addAction(localRef, act)
        modifier.actions += act
      }
    })

    blocks.filter(_.isInstanceOf[parser.TemporalConstraint]).map(_.asInstanceOf[parser.TemporalConstraint]).foreach(constraint => {
      val abs = AbstractTemporalConstraint(constraint)
      modifier.temporalConstraints += TemporalConstraint(this, context, abs)
    })

    modifiers += modifier
  }

}
