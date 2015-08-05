package planstack.anml.model

import java.io.File

import planstack.anml.model.abs.{AbstractAction, AbstractTemporalConstraint, StatementsFactory}
import planstack.anml.model.concrete._
import planstack.anml.parser.{ANMLFactory, ParseResult}
import planstack.anml.{ANMLException, parser}


import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

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
  * Further more an ANML problem consists of a list of [[planstack.anml.model.concrete.Chronicle]]
  * representing everything that must be added to plan solving this problem.
  * A first one is added in the constructor containing predefined instances of the problem (such as true and false).
  * Every time an `addAnml(...)` method it called, the problem's components are updated accordingly and a new
  * [[planstack.anml.model.concrete.Chronicle]] is added to represent the changes in the problems.
  *
  * @param usesActionConditions If set to true, the ANML problem will use ActionCondition instead of Action for representing subtasks.
  */
class AnmlProblem(val usesActionConditions : Boolean) extends TemporalInterval {

  val refCounter = new RefCounter()

  /**
   * A time-point representing the earliest possible execution of an action.
   * Note that no explicit constraint including this timepoint is specified in the the ANML problem, it
   * is provided here mainly for consistency in the planner implementation. It is the responsability of the
   * planner to enforce the ordering between this time-point and the actions in the plan.
   */
  val earliestExecution : TPRef = new TPRef(refCounter)
  override val start: TPRef = new TPRef(refCounter)
  override val end: TPRef = new TPRef(refCounter)

  /**
   * An InstanceManager that keeps track of all types and instances of the problem.
   */
  val instances = new InstanceManager(refCounter)

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
   * All [[planstack.anml.model.concrete.Chronicle]] that need to be applied to a state for it to represent this problem.
   * There is one chronicle encoding the default definitions of an ANML problem (such as the instances true and false of type
   * boolean). One chronicle is added by update of the problem (as a result of the invocation of `addAnml(...)`.
   */
  val chronicles = new java.util.LinkedList[Chronicle]()

  // create an initial chronicle containing the predefined instances (true and false)
  {
    val initialChronicle = new BaseChronicle(this)

    // add predefined instance to context and to StateModifier
    for((name, tipe) <- instances.instances) {
      initialChronicle.instances += name
      context.addVar(new LVarRef(name), tipe, instances.referenceOf(name))
    }

    chronicles += initialChronicle
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

  /** Returns true if this problem definition contains an action with the given name */
  def containsAction(name:String) : Boolean = abstractActions.find(_.name == name) match {
    case Some(act) => true
    case None => false
  }

  /**
   * Extends this problem with the ANML found in the file.
   * It the file name is formatted as xxxxxx.yyy.pb.anml, it will load the file
   * xxxxxx.dom.anml as well.
   * If any updates need to be made to a search state as a consequence,
   * those are encoded as a Chronicle and added to `chronicles`
   * @param filename File in which the anml text can be found.
   */
  def extendWithAnmlFile(filename: String) : Unit = {
    if(filename.endsWith(".pb.anml")) {
      val f = new File(filename)
      f.getName.split("\\.").toList match {
        case base :: num :: "pb" :: "anml" :: Nil =>
          addAnml(ANMLFactory.parseAnmlFromFile(new File(f.getParentFile, base+".dom.anml").getAbsolutePath))
        case _ =>
          throw new ANMLException("Error: file name does not follow the convention: "+filename+"."+
           "It should be in the form domainName.xxx.pb.anml and have an associated domainName.dom.anml file.")
      }
    }
    addAnml(ANMLFactory.parseAnmlFromFile(filename))
  }

  /**
   * Extends this problem with the ANML found in the string.
   * If any updates need to be made to a search state as a consequence,
   * those are encoded as a Chronicle and added to `chronicles`
   * @param anml An anml string.
   */
  def extendWithAnmlText(anml: String) : Unit = {
    addAnml(ANMLFactory.parseAnmlString(anml))
  }

  /**
   * Integrates new ANML blocks into the problem. If any updates need to be made to a search state as a consequence,
   * those are encoded as a Chronicle and added to `chronicles`
   * @param anml Output of the ANML parser for the ANML block.
   */
  private def addAnml(anml:ParseResult) = addAnmlBlocks(anml.blocks)

  /**
   * Integrates new ANML blocks into the problem. If any updates need to be made to a search state as a consequence,
   * those are encoded as a Chronicle and added to `chronicles`
   * @param blocks A sequence of ANML blocks.
   */
  private def addAnmlBlocks(blocks:Seq[parser.AnmlBlock]) {

    // chronicle that containing all alterations to be made to a plan as a consequence of this ANML block
    val chronicle = new BaseChronicle(this)

    // add all type declarations to the instance manager.
    blocks.filter(_.isInstanceOf[parser.Type]).map(_.asInstanceOf[parser.Type]) foreach(typeDecl => {
      instances.addType(typeDecl.name, typeDecl.parent)
    })

    // add all instance declaration to the instance manager and to the chronicle
    blocks.filter(_.isInstanceOf[parser.Instance]).map(_.asInstanceOf[parser.Instance]) foreach(instanceDecl => {
      instances.addInstance(instanceDecl.name, instanceDecl.tipe, refCounter)
      chronicle.instances += instanceDecl.name
      // all instances are added to the context
      context.addVar(new LVarRef(instanceDecl.name), instanceDecl.tipe, instances.referenceOf(instanceDecl.name))
    })

    // add all functions to the function manager
    blocks.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]) foreach(funcDecl => {
      assert(!funcDecl.name.contains("."), "Declaring function "+funcDecl+" is not supported. If you wanted to " +
        "declared a function linked to type, you should do so in the type itself.") // TODO: should be easy to support

      if(funcDecl.args.isEmpty && funcDecl.isConstant) {
        // declare as a variable since it as no argument and is constant.
        val newVar = new VarRef(refCounter)
        context.addVar(LVarRef(funcDecl.name), funcDecl.tipe, newVar)
        chronicle.vars += ((funcDecl.tipe, newVar))
      } else {
        // either non-constant or with arguments
        functions.addFunction(funcDecl)
      }
    })

    // find all methods declared inside a type and them to functions and to the type.
    blocks.filter(_.isInstanceOf[parser.Type]).map(_.asInstanceOf[parser.Type]) foreach(typeDecl => {
      typeDecl.content.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]).foreach(scopedFunction => {
        functions.addScopedFunction(typeDecl.name, scopedFunction)
        instances.addMethodToType(typeDecl.name, scopedFunction.name)
      })
    })

    blocks.filter(_.isInstanceOf[parser.Action]).map(_.asInstanceOf[parser.Action]) foreach(actionDecl => {
      val abs = AbstractAction(actionDecl, this, refCounter)
      abstractActions += abs

      // if the action is a seed, add it to the chronicle to make sure it appears in the initial plan.
      if(abs.name == "Seed" || abs.name == "seed") {
        throw new ANMLException("Seed action is depreciated.")
      }
    })

    blocks.filter(_.isInstanceOf[parser.TemporalStatement]).map(_.asInstanceOf[parser.TemporalStatement]) foreach(tempStatement => {
      val absStatements = StatementsFactory(tempStatement, this.context, this, refCounter)
      chronicle.addAll(absStatements, context, this, refCounter)
    })

    blocks.filter(_.isInstanceOf[parser.TemporalConstraint]).map(_.asInstanceOf[parser.TemporalConstraint]).foreach(constraint => {
      val abs = AbstractTemporalConstraint(constraint)
      chronicle.temporalConstraints += TemporalConstraint(this, context, abs)
    })

    chronicles += chronicle
  }

  /**
   * Creates a chronicle from the ANML found in the file.
   * The context (instances, action, functions, ...) of this chronicle is the
   * one defined in this problem. However the problem will not be updated.
   * Hence any declaration of action, type, function or instance will fail.
   * @return The chronicle representing the ANML text.
   */
  def getChronicleFromFile(filename: String) : Chronicle =
    getChronicle(ANMLFactory.parseAnmlFromFile(filename).blocks)

  /**
   * Creates a chronicle from the ANML found in the string.
   * The context (instances, action, functions, ...) of this chronicle is the
   * one defined in this problem. However the problem will not be updated.
   * Hence any declaration of action, type, function or instance will fail.
   * @return The chronicle representing the ANML text.
   */
  def getChronicleFromAnmlText(anml: String) : Chronicle =
    getChronicle(ANMLFactory.parseAnmlString(anml).blocks)

  private def getChronicle(blocks:Seq[parser.AnmlBlock]) : Chronicle = {
    val chron = new BaseChronicle(this)

    // this context is declared locally to avoid polluting the problem's context
    // they have the same interval so start/end map to the ones of the problem
    val localContext = new Context(Some(this.context))
    localContext.setInterval(this.context.interval)

    // first process variable definitions to make them available (in local context)
    // to all other statements
    for(block <- blocks.filter(_.isInstanceOf[parser.Function])) block match {
      // this is a variable that we should be able to use locally
      case func: parser.Function if func.args.isEmpty && func.isConstant =>
        val newVar = new VarRef(refCounter)
        localContext.addVar(LVarRef(func.name), func.tipe, newVar)
        chron.vars += ((func.tipe, newVar))

      // complete function definition, would change the problem.
      case _ =>
        throw new ANMLException("Declaration of functions is not allow as it would modify the problem.")
    }

    for(block <- blocks.filter(!_.isInstanceOf[parser.Function])) block match {
      case ts: parser.TemporalStatement =>
        val absStatements = StatementsFactory(ts, localContext, this, refCounter)
        chron.addAll(absStatements, localContext, this, refCounter)

      case tc: parser.TemporalConstraint =>
        val abs = AbstractTemporalConstraint(tc)
        chron.temporalConstraints += TemporalConstraint(this, localContext, abs)

      case _ =>
        throw new ANMLException("Cannot integrate the following block into the chronicle as it would "+
          "change the problem definition: "+block)
    }

    chron
  }

  def instance(instanceName: String) : InstanceRef = instances.referenceOf(instanceName)

  /** Builds a state variable with the given function and args */
  def stateVariable(funcName: String, args: Seq[String]) = {
    val vars = args.map(instances.referenceOf(_))
    val func = functions.get(funcName)
    new ParameterizedStateVariable(func, vars.toArray)
  }

  /** Builds a state variable with the given function and args */
  def jStateVariable(funcName: String, args: java.util.List[String]) =
    stateVariable(funcName, args.asScala)
}
