package planstack.anml.model

import java.io.File
import java.{util => ju}

import planstack.anml.model.abs._
import planstack.anml.model.abs.statements.AbstractStatement
import planstack.anml.model.abs.time.{AbsTP, ContainerEnd, ContainerStart, TimepointTypeEnum}
import planstack.anml.model.concrete._
import planstack.anml.parser.{ANMLFactory, PSimpleType, ParseResult, TypeDecl}
import planstack.anml.{ANMLException, parser}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** Description of an ANML problem.
  *
  * An ANML problem comes with three time points: start (global start of the world the problem takes place in),
  * end (global end of the world) and earliestExecution which represent the earliest time at which an action can be executed.
  * Temporal intervals (such as actions, statements, ...) are placed relatively to the start and end time points using
  * [[planstack.anml.model.concrete.MinDelayConstraint]].
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
  */
class AnmlProblem extends TemporalInterval with ChronicleContainer {

  def label = "Problem"
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
  val functions = new FunctionManager(this)

  /** Context that all variables and and action that appear in the problem scope.
    * Those typically contain instances of the problem and predefined ANML literals (such as true, false, ...)
    */
  val context = new Context(this, "problem", None, this)

  /** All abstract actions appearing in the problem */
  val abstractActions = new ju.LinkedList[AbstractAction]()

  lazy val allActionsAreMotivated = abstractActions.asScala.forall(aa => aa.isTaskDependent)

  val actionsByTask = mutable.Map[String, ju.List[AbstractAction]]().asJava
  /** contains all tasks name together with their number of arguments */
  val tasks = mutable.Map[String,List[parser.Argument]]()
  val tasksMinDurations = mutable.Map[String,Int]()

  /**
   * All [[planstack.anml.model.concrete.Chronicle]] that need to be applied to a state for it to represent this problem.
   * There is one chronicle encoding the default definitions of an ANML problem (such as the instances true and false of type
   * boolean). One chronicle is added by update of the problem (as a result of the invocation of `addAnml(...)`.
   */
  val chronicles = new java.util.LinkedList[Chronicle]()

  // create an initial chronicle containing the predefined instances (true and false)
  {
    val abstractChronicle = EmptyAbstractChronicle
      .withConstantDeclarations(instances.allInstances.map(i => (EVariable(i, instances.typeOf(i)), instances.referenceOf(i))).toSeq)
      .withConstraints(new AbstractMinDelay(ContainerStart,ContainerEnd, 0))
      .withConstraints(new AbstractTimepointType(ContainerStart,TimepointTypeEnum.DISPATCHABLE))
      .withConstraints(new AbstractTimepointType(ContainerEnd,TimepointTypeEnum.DISPATCHABLE))


    val c = abstractChronicle.getInstance(context, this, this, refCounter)
    c.container = Some(this)
    chronicles += c
  }

  /**
   * Retrieves the abstract action with the given name.
    *
    * @param name Name of the action to lookup.
   * @return The corresponding AbstractAction.
   */
  def getAction(name:String) = abstractActions.find(_.name == name) match {
    case Some(act) => act
    case None => throw new ANMLException("No action named "+name)
  }

  def getSupportersForTask(taskName: String) : ju.List[AbstractAction] = {
    assert(actionsByTask.contains(taskName), "Task: "+taskName+" is not registered.")
    assert(actionsByTask(taskName).nonEmpty, "There is no action registered for task: "+taskName)
    actionsByTask(taskName)
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
    *
    * @param filename File in which the anml text can be found.
   */
  def extendWithAnmlFile(filename: String) : Unit = {
    if(filename.endsWith(".pb.anml")) {
      val f = new File(filename)
      f.getName.split("\\.").toList match {
        case base :: num :: "pb" :: "anml" :: Nil =>
          val domainFile = new File(f.getParentFile, base+".dom.anml")
          addAnml(ANMLFactory.parseAnmlFromFiles(List(domainFile.getAbsolutePath, filename)))
        case _ =>
          throw new ANMLException("Error: file name does not follow the convention: "+filename+"."+
           "It should be in the form domainName.xxx.pb.anml and have an associated domainName.dom.anml file.")
      }
    } else {
      addAnml(ANMLFactory.parseAnmlFromFile(filename))
    }
  }

  /**
   * Extends this problem with the ANML found in the string.
   * If any updates need to be made to a search state as a consequence,
   * those are encoded as a Chronicle and added to `chronicles`
    *
    * @param anml An anml string.
   */
  def extendWithAnmlText(anml: String) : Unit = {
    addAnml(ANMLFactory.parseAnmlString(anml))
  }

  /**
   * Integrates new ANML blocks into the problem. If any updates need to be made to a search state as a consequence,
   * those are encoded as a Chronicle and added to `chronicles`
    *
    * @param anml Output of the ANML parser for the ANML block.
   */
  private def addAnml(anml:ParseResult) = addAnmlBlocks(anml.blocks)

  /**
   * Integrates new ANML blocks into the problem. If any updates need to be made to a search state as a consequence,
   * those are encoded as a Chronicle and added to `chronicles`
    *
    * @param blocks A sequence of ANML blocks.
   */
  private def addAnmlBlocks(blocks:Seq[parser.AnmlBlock]) {

    // chronicle that containing all alterations to be made to a plan as a consequence of this ANML block
    var chron : AbstractChronicle = EmptyAbstractChronicle

    // add all type declarations to the instance manager.
    blocks.filter(_.isInstanceOf[parser.TypeDecl]) foreach {
      case TypeDecl(PSimpleType(name), None, _) => instances.addType(name, "")
      case TypeDecl(PSimpleType(name), Some(PSimpleType(parent)), _) => instances.addType(name, parent)
    }

    // add all instance declaration to the instance manager and to the chronicle
    blocks collect {
      case parser.Instance(PSimpleType(typeName), name) =>

        instances.addInstance(name, typeName, refCounter)
        // all instances are added to the context
        val inst = instance(name)
        val locVar = EVariable(name, inst.getType)
        context.addVar(locVar,inst)
        chron = chron.withConstantDeclarations((locVar, inst) :: Nil)
    }

    // add all functions to the function manager
    blocks collect { case funcDecl:parser.Function =>
      assert(!funcDecl.name.contains("."), "Declaring function "+funcDecl+" is not supported. If you wanted to " +
        "declared a function linked to type, you should do so in the type itself.") // TODO: should be easy to support

      if(funcDecl.args.isEmpty && funcDecl.isConstant) {
        // declare as a variable since it as no argument and is constant.
        val locVar = EVariable(funcDecl.name, instances.asType(funcDecl.tipe))
        context.addUndefinedVar(locVar)
        chron = chron.withVariableDeclarations(locVar :: Nil)
      } else {
        // either non-constant or with arguments
        functions.addFunction(funcDecl)
      }
    }

    // find all methods declared inside a type and them to functions and to the type.
    blocks collect { case typeDecl: parser.TypeDecl =>
      typeDecl.content.filter(_.isInstanceOf[parser.Function]).map(_.asInstanceOf[parser.Function]).foreach(scopedFunction => {
        functions.addScopedFunction(instances.asType(typeDecl.name), scopedFunction)
        instances.asType(typeDecl.name).addMethod(scopedFunction.name)
      })
    }

    // record all tasks (needed when processing statements)
    blocks collect { case parser.Action(name, args, _) =>
      assert(!tasks.contains(name), s"Action \'$name\' is already defined.")
      tasks.put(name, args)
    }

    blocks collect { case actionDecl: parser.Action =>
      val abs = AbstractAction(actionDecl, this, refCounter)
      assert(abs.nonEmpty)
      val task = abs.head.taskName
      assert(!actionsByTask.contains(task), "Task \""+task+"\" is already registered. Maybe the corresponding action was declared twice.")
      abstractActions ++= abs
      actionsByTask += ((task, abs.asJava))
    }

    blocks collect { case tempStatement: parser.TemporalStatement =>
      chron += StatementsFactory(tempStatement, this.context, refCounter, DefaultMod)
    }

    blocks collect { case constraint: parser.TemporalConstraint =>
      chron = chron.withConstraintsSeq(AbstractTemporalConstraint(constraint))
    }

    blocks collect { case parser.ForAll(args, content) =>
      // need an all combinations
      val domains = args.map(a => instances.asType(a.tipe).instances.map(i => (a.name, i.instance)).toList)
      def combinations[E](ll: List[List[E]]) : List[List[E]] =
        ll match {
          case Nil => Nil
          case l::Nil => l.map(x => List(x))
          case h::t => for(i <- h ; rest <- combinations(t)) yield i::rest
        }
      val combis = combinations(domains)
      for(binding <- combis) {
        val transformationMap : Map[String,String] = binding.toMap
        content collect {
          case ts: parser.TemporalStatement =>
            chron += StatementsFactory(ts, this.context, refCounter, new Mod {
              def varNameMod(name:String) = transformationMap.getOrElse(name, name)
              def idModifier(s:String) = DefaultMod.idModifier(s)
            })
        }
      }
    }

    blocks collect { case parser.ObservationConditionsAnnotation(tpName, content) =>
      var conditions : AbstractChronicle = EmptyAbstractChronicle
      val tp = AbsTP(tpName)
      content collect {
        case ts: parser.TemporalStatement =>
          val ac = StatementsFactory(ts, this.context, refCounter)
          conditions += ac
        case x => throw new ANMLException(s"The use of '$x' is not supported inside an ObservationConditions annotation")
      }
      chron += new AbstractObservationConditionsAnnotation(tp, conditions)
    }
//
//    for((function,variable) <- context.bindings if !function.func.valueType.isNumeric) {
//      val sv = new AbstractParameterizedStateVariable(function.func, function.args)
//      absConstraints += new AbstractEqualityConstraint(sv, variable, LStatementRef(""))
//    }

    val c = chron.getInstance(context, this, this, refCounter)
    c.container = Some(this)
    chronicles += c
  }

  /**
   * Creates a chronicle from the ANML found in the file.
   * The context (instances, action, functions, ...) of this chronicle is the
   * one defined in this problem. However the problem will not be updated.
   * Hence any declaration of action, type, function or instance will fail.
 *
   * @return The chronicle representing the ANML text.
   */
  def getChronicleFromFile(filename: String) : Chronicle =
    getChronicle(ANMLFactory.parseAnmlFromFile(filename).blocks)

  /**
   * Creates a chronicle from the ANML found in the string.
   * The context (instances, action, functions, ...) of this chronicle is the
   * one defined in this problem. However the problem will not be updated.
   * Hence any declaration of action, type, function or instance will fail.
 *
   * @return The chronicle representing the ANML text.
   */
  def getChronicleFromAnmlText(anml: String) : Chronicle =
    getChronicle(ANMLFactory.parseAnmlString(anml).blocks)

  private def getChronicle(blocks:Seq[parser.AnmlBlock]) : Chronicle = {
    val chron = new Chronicle(this)

    // this context is declared locally to avoid polluting the problem's context
    // they have the same interval so start/end map to the ones of the problem
    val localContext = new Context(this, chron.getLabel, Some(this.context), this.context.interval)

    // first process variable definitions to make them available (in local context)
    // to all other statements
    blocks.filter(_.isInstanceOf[parser.Function]) foreach {
      // this is a variable that we should be able to use locally
      case func: parser.Function if func.args.isEmpty && func.isConstant =>
        val newVar = new VarRef(instances.asType(func.tipe), refCounter, Label(chron.getLabel,func.name))
        localContext.addVar(EVariable(func.name, instances.asType(func.tipe)), newVar)
        chron.vars += newVar

      // complete function definition, would change the problem.
      case _ =>
        throw new ANMLException("Declaration of functions is not allowed as it would modify the problem.")
    }

    var absChronicle : AbstractChronicle = EmptyAbstractChronicle

    blocks.filter(!_.isInstanceOf[parser.Function]) foreach {
      case ts: parser.TemporalStatement =>
        val ac = StatementsFactory(ts, localContext, refCounter, DefaultMod)
        absChronicle += ac

      case tc: parser.TemporalConstraint =>
        absChronicle = absChronicle.withConstraintsSeq(AbstractTemporalConstraint(tc))

      case block =>
        throw new ANMLException("Cannot integrate the following block into the chronicle as it would "+
          "change the problem definition: "+block)
    }

    absChronicle.getInstance(context, this, this, refCounter)
  }


  def instance(instanceName: String) : InstanceRef = instances.referenceOf(instanceName)

  /** Builds a state variable with the given function and args */
  def stateVariable(funcName: String, args: Seq[String]) = {
    val vars = args.map(instances.referenceOf)
    val func = functions.get(funcName)
    new ParameterizedStateVariable(func, vars.toArray)
  }

  /** Builds a state variable with the given function and args */
  def jStateVariable(funcName: String, args: java.util.List[String]) =
    stateVariable(funcName, args.asScala)
}
