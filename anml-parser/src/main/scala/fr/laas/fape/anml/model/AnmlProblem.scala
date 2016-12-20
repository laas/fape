package fr.laas.fape.anml.model

import java.io.File
import java.{util => ju}

import fr.laas.fape.anml
import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.model.abs.statements.AbstractStatement
import fr.laas.fape.anml.model.abs.time.{AbsTP, ContainerEnd, ContainerStart, TimepointTypeEnum}
import fr.laas.fape.anml.model.abs._
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.anml.model.ir.IRSimpleVar
import fr.laas.fape.anml.parser._
import fr.laas.fape.anml.parser

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** Description of an ANML problem.
  *
  * An ANML problem comes with three time points: start (global start of the world the problem takes place in),
  * end (global end of the world) and earliestExecution which represent the earliest time at which an action can be executed.
  * Temporal intervals (such as actions, statements, ...) are placed relatively to the start and end time points using
  * [[MinDelayConstraint]].
  * The earliestExecution time point will not have any explicit constraints linked to it (we consider this choice is
  * plannner-dependent and is not explicitly defined in ANML).
  *
  * Components include:
  *
  *  - an [[fr.laas.fape.anml.model.InstanceManager]] that keeps track of all types and instances declared
  * in the problem.
  *
  *  - a [[fr.laas.fape.anml.model.FunctionManager]] that records of all functions declared in the ANML problem.
  *
  *  - a [[Context]] that map local variables and references that appear in the problem
  *  scope to global ones.
  *
  *
  * Further more an ANML problem consists of a list of [[fr.laas.fape.anml.model.concrete.Chronicle]]
  * representing everything that must be added to plan solving this problem.
  * A first one is added in the constructor containing predefined instances of the problem (such as true and false).
  * Every time an `addAnml(...)` method it called, the problem's components are updated accordingly and a new
  * [[fr.laas.fape.anml.model.concrete.Chronicle]] is added to represent the changes in the problems.
  */
class AnmlProblem extends TemporalInterval with ChronicleContainer {

  def label = "Problem"
  val refCounter = RefCounter.getNewCounter

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
   * A [[fr.laas.fape.anml.model.FunctionManager]] that keeps track of all functions (ie. definition of state variables)
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
  val tasks = mutable.Map[String,List[Argument]]()
  val tasksMinDurations = mutable.Map[String,Int]()

  /**
   * All [[fr.laas.fape.anml.model.concrete.Chronicle]] that need to be applied to a state for it to represent this problem.
   * There is one chronicle encoding the default definitions of an ANML problem (such as the instances true and false of type
   * boolean). One chronicle is added by update of the problem (as a result of the invocation of `addAnml(...)`.
   */
  val chronicles = new java.util.LinkedList[Chronicle]()

  // create an initial chronicle containing the predefined instances (true and false)
  {
    val abstractChronicle = EmptyAbstractChronicle
      .withConstantDeclarations(instances.allInstances.map(i => (IRSimpleVar(i, instances.typeOf(i)), instances.referenceOf(i))).toSeq)
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
    val files =
      if(filename.endsWith(".pb.anml")) {
        val f = new File(filename)
        f.getName.split("\\.").toList match {
          case base :: num :: "pb" :: "anml" :: Nil =>
            val domainFile = new File(f.getParentFile, base+".dom.anml")
            List(domainFile.getAbsolutePath, filename)
          case _ =>
            throw new ANMLException("Error: file name does not follow the convention: "+filename+"."+
              "It should be in the form domainName.xxx.pb.anml and have an associated domainName.dom.anml file.")
        }
      } else {
        List(filename)
      }
    val anmlText = files.foldLeft("")((acc, filePath) => {
      val reader = scala.io.Source.fromFile(filePath)
      val fileContent = reader.mkString
      reader.close()
      acc+"\n"+fileContent
    })
    extendWith(anmlText)
  }

  /**
    * Extends this problem definition with the given anml string.
 *
    * @param anmlString ANML code to incorporate
    */
  def extendWith(anmlString: String): Unit = {
    val parseResult = ANMLFactory.parseAnmlString(anmlString)
    val chronicle = anmlBlocksAsChronicle(parseResult.blocks, allowProblemUpdate = true)
    chronicles += chronicle
    chronicle.container = Some(this)
  }

  /**
    * Builds a new chronicle based on an ANML string, using the context of this problem.
    * The problem is not updated.
 *
    * @param anmlString ANML code to transform
    * @return A new chronicle, co
    * @throws ANMLException if the ANML string requires the problem to be modified (e.g. declares a new type,...)
    */
  def asChronicle(anmlString: String): Chronicle = {
    val parseResult = ANMLFactory.parseAnmlString(anmlString)
    anmlBlocksAsChronicle(parseResult.blocks, allowProblemUpdate = false)
  }

  /**
    * Builds a new chronicle based on the given anml blocks.
 *
    * @param blocks A sequence of ANML block to appear in the chronicle
    * @param allowProblemUpdate If true, allows the current problem to be updated (e.g. to include a new type declaration found in the blocks)
    * @return A chronicle containing all statements in the blocks
    * @throws ANMLException if the problem requires an update and those are not allowed
    */
  private def anmlBlocksAsChronicle(blocks:Seq[AnmlBlock], allowProblemUpdate: Boolean): Chronicle = {
    // sort blocks to make sure they are processed in the right order (types, instances and functions first)
    val sorted = blocks.sortBy {
      case _: TypeDeclaration => 0
      case _: parser.Instance => 1
      case _: parser.Function => 2
      case _ => 3
    }
    var chronicle : AbstractChronicle = EmptyAbstractChronicle
    for(block <- sorted)
      chronicle = extendChronicleWithBlock(block, chronicle, allowProblemUpdate)

    chronicle.getInstance(context, this, this, refCounter)
  }

  /**
    * Incorporates an ANML block into a chronicle.
 *
    * @param block Block to integrate
    * @param baseChronicle Base chronicle to extend with the given block
    * @param allowProblemUpdate If true, allows the current problem to be updated (e.g. to include a new type declaration found in the block)
    * @return The extended chronicle
    * @throws ANMLException if the problem requires an update and those are not allowed
    */
   private def extendChronicleWithBlock(block:AnmlBlock, baseChronicle: AbstractChronicle, allowProblemUpdate: Boolean) : AbstractChronicle = {
     var chron = baseChronicle

     block match {
       case tempStatement: TemporalStatement =>
         chron += StatementsFactory(tempStatement, this.context, refCounter, DefaultMod)

       case constraint: anml.parser.TemporalConstraint =>
         chron = chron.withConstraintsSeq(AbstractTemporalConstraint(constraint))

       case parser.ForAll(args, content) =>
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
             case ts: TemporalStatement =>
               chron += StatementsFactory(ts, this.context, refCounter, new Mod {
                 def varNameMod(name:String) = transformationMap.getOrElse(name, name)
                 def idModifier(s:String) = DefaultMod.idModifier(s)
               })
           }
         }

       case parser.ObservationConditionsAnnotation(tpName, content) =>
         var conditions : AbstractChronicle = EmptyAbstractChronicle
         val tp = AbsTP(tpName)
         content collect {
           case ts: TemporalStatement =>
             val ac = StatementsFactory(ts, this.context, refCounter)
             conditions += ac
           case x => throw new ANMLException(s"The use of '$x' is not supported inside an ObservationConditions annotation")
         }
         chron += new AbstractObservationConditionsAnnotation(tp, conditions)

       // all following block require to modify the ANML problem, check that we have the right to do so
       case x if !allowProblemUpdate =>
         throw new ANMLException(s"The ANML block $x is either not supported or would require extending the domain definition")

       // add all type declarations to the instance manager.
       case TypeDeclaration(PSimpleType(name), None) =>
         instances.addType(name, "")
       case TypeDeclaration(PSimpleType(name), Some(PSimpleType(parent))) =>
         instances.addType(name, parent)

       // add all instance declaration to the instance manager and to the chronicle
       case parser.Instance(PSimpleType(typeName), name) =>
         instances.addInstance(name, typeName, refCounter)
         // all instances are added to the context
         val inst = instance(name)
         val locVar = IRSimpleVar(name, inst.getType)
         context.addVar(locVar,inst)
         chron = chron.withConstantDeclarations((locVar, inst) :: Nil)

       // add all functions to the function manager
       case funcDecl:parser.Function =>
         funcDecl.name.split("\\.").toList match {
           case containingType :: funcName :: Nil => // this is funciton declared in a type
             functions.addFunction(funcDecl)
             instances.asType(containingType).addMethod(funcName)
           case funcName :: Nil => // function independant of any type
             if(funcDecl.args.isEmpty && funcDecl.isConstant) {
               // declare as a variable since it as no argument and is constant.
               val locVar = IRSimpleVar(funcDecl.name, instances.asType(funcDecl.tipe))
               context.addUndefinedVar(locVar)
               chron = chron.withVariableDeclarations(locVar :: Nil)
             } else {
               // either non-constant or with arguments
               functions.addFunction(funcDecl)
             }
           case xs => throw new ANMLException(s"Error: function with multiple levels of nesting: $funcDecl")
         }

       // record all tasks (needed when processing statements)
       case action: parser.Action =>
         assert(!tasks.contains(action.name), s"Action \'${action.name}\' is already defined.")
         tasks.put(action.name, action.args)
         val abs = AbstractAction(action, this, refCounter)
         assert(abs.nonEmpty)
         val task = abs.head.taskName
         assert(!actionsByTask.contains(task), "Task \""+task+"\" is already registered. Maybe the corresponding action was declared twice.")
         abstractActions ++= abs
         actionsByTask += ((task, abs.asJava))
     }

     chron // return extended chronicle
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

  private def getChronicle(blocks:Seq[AnmlBlock]) : Chronicle = {
    val chron = new Chronicle()

    // this context is declared locally to avoid polluting the problem's context
    // they have the same interval so start/end map to the ones of the problem
    val localContext = new Context(this, chron.getLabel, Some(this.context), this.context.interval)

    // first process variable definitions to make them available (in local context)
    // to all other statements
    blocks.filter(_.isInstanceOf[anml.parser.Function]) foreach {
      // this is a variable that we should be able to use locally
      case func: anml.parser.Function if func.args.isEmpty && func.isConstant =>
        val newVar = new VarRef(instances.asType(func.tipe), refCounter, Label(chron.getLabel,func.name))
        localContext.addVar(IRSimpleVar(func.name, instances.asType(func.tipe)), newVar)
        chron.vars += newVar

      // complete function definition, would change the problem.
      case _ =>
        throw new ANMLException("Declaration of functions is not allowed as it would modify the problem.")
    }

    var absChronicle : AbstractChronicle = EmptyAbstractChronicle

    blocks.filter(!_.isInstanceOf[anml.parser.Function]) foreach {
      case ts: TemporalStatement =>
        val ac = StatementsFactory(ts, localContext, refCounter, DefaultMod)
        absChronicle += ac

      case tc: anml.parser.TemporalConstraint =>
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
