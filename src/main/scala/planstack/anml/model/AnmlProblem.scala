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
  val context = new Context(None)
  context.setInterval(this)

  val abstractActions = ListBuffer[AbstractAction]()
  def jAbstractActions = seqAsJavaList(abstractActions)

  /**
   * All [[planstack.anml.model.concrete.StateModifier]] that need to be applied to a state for it to reprent this problem.
   * There is one modifier encoding the default definitions of an ANML problem (such as the instances true and false of type
   * boolean). One modifier is added by update of the problem (as a result of the invocation of `addAnml(...)`.
   */
  val modifiers = ArrayBuffer[StateModifier]()
  def jModifiers = seqAsJavaList(modifiers)

  private var nextGlobalVarID = 0
  private var nextActionID = 0

  // create an initial modifier containing the predefined instances (true and false)
  {
    val originalSM = new BaseStateModifier(this)
    originalSM.instances ++= instances.instances.map(_._1)
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

    val modifier = new BaseStateModifier(this)

    blocks.filter(_.isInstanceOf[parser.Type]).map(_.asInstanceOf[parser.Type]) foreach(typeDecl => {
      instances.addType(typeDecl.name, typeDecl.parent)
    })

    blocks.filter(_.isInstanceOf[parser.Instance]).map(_.asInstanceOf[parser.Instance]) foreach(instanceDecl => {
      instances.addInstance(instanceDecl.name, instanceDecl.tipe)
      modifier.instances += instanceDecl.name
    })

    for((name, tipe) <- instances.instances) {
      context.addVar(new LVarRef(name), tipe, instances.referenceOf(name))
    }

    blocks.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]) foreach(funcDecl => {
      functions.addFunction(funcDecl)
    })

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
