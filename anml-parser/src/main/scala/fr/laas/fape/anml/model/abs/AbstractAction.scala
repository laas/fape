package fr.laas.fape.anml.model.abs

import java.util

import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.parser
import fr.laas.fape.anml.model.abs.statements._
import fr.laas.fape.anml.model.abs.time.{AbsTP, ContainerEnd, ContainerStart, TimepointTypeEnum}
import fr.laas.fape.anml.model.concrete.RefCounter
import fr.laas.fape.anml.model.{abs, _}
import fr.laas.fape.anml.parser._
import fr.laas.fape.anml.pending.IntExpression

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/** An abstract action is an representation for an action as it is defined in an ANML problem.
  * It gives, for an action, an abstract view of what it does, regardless of the parameters it will be given when instantiated.
  *
  * Hence all components of an action refer either to local references (such as an argument of the action) or problem
  * instances (defined in the ANML problem).
  */
class AbstractAction(val baseName: String,
                     val taskName: String,
                     val decID: Int,
                     private val mArgs: List[LVarRef],
                     private val taskDependent: Boolean,
                     val context:PartialContext,
                     val chron: AbstractChronicle)  {


  val name =
    if(decID == 0) baseName
    else "m"+decID+"-"+baseName

  /** True if the action was defined with the motivated keyword. False otherwise. */
  def isTaskDependent = taskDependent

  /** Arguments in the form of local references containing the name of the argument */
  def args = seqAsJavaList(mArgs)

  /** All abstract temporal statements appearing in this action */
  val statements = chron.getStatements
  val constraints = chron.allConstraints

  assert(chron.optSTNU.nonEmpty, "Missing preprocessing phase of temporal constraints in action "+baseName)
  def flexibleTimepoints = chron.optSTNU.get.flexibleTimepoints
  def anchoredTimepoints = chron.optSTNU.get.anchoredTimepoints
  def stn = chron.optSTNU.get.stn


  def start = ContainerStart
  def end = ContainerEnd

  def subTasks = chron.subTasks
  lazy val logStatements = statements.filter(_.isInstanceOf[AbstractLogStatement]).map(_.asInstanceOf[AbstractLogStatement])
  lazy val resStatements = statements.filter(_.isInstanceOf[AbstractResourceStatement]).map(_.asInstanceOf[AbstractResourceStatement])

  /** Java friendly version of [[abs.AbstractAction#temporalStatements]]. */
  def jStatements = seqAsJavaList(statements)
  def jConstraints = seqAsJavaList(constraints)

  def jSubTasks = seqAsJavaList(subTasks)
  def jLogStatements = seqAsJavaList(logStatements)
  def jResStatements = seqAsJavaList(resStatements)

  def getLogStatement(ref: LStatementRef) = logStatements.find(s => s.id == ref).getOrElse({ throw new ANMLException("No statement with this ref.") })

  lazy private val _allVars : Array[LVarRef] = chron.allVariables.toArray
  def allVars : Array[LVarRef] = {
    _allVars
  }

  def minDelay(from: AbsTP, to: AbsTP) = stn.minDelay(from,to)

  def maxDelay(from: AbsTP, to: AbsTP) = stn.maxDelay(from, to)

  private val achievementsAt = new util.HashMap[AbsTP, util.List[(AbstractFluent, AbstractLogStatement)]]()
  private val conditionsAt = new util.HashMap[AbsTP, util.List[(AbstractFluent, AbstractLogStatement)]]()
  private val changesFrom = new util.HashMap[AbsTP, util.List[AbstractLogStatement]]()

  def getAchievementsAt(tp: AbsTP) = {
    if(!achievementsAt.containsKey(tp)) {
      val list : util.List[(AbstractFluent, AbstractLogStatement)] =
        logStatements
          .filter(s => s.hasEffectAtEnd)
          .filter(s => stn.concurrent(tp, s.end))
          .map(s => (AbstractFluent(s.sv, s.effectValue), s))
      achievementsAt += ((tp, list))
    }
    achievementsAt(tp)
  }

  def getConditionsAt(tp: AbsTP) = {
    if(!conditionsAt.containsKey(tp)) {
      val list : util.List[(AbstractFluent, AbstractLogStatement)] =
        logStatements
          .flatMap(s => s match {
            case t: AbstractTransition if stn.concurrent(tp, t.start) => List(t)
            case p: AbstractPersistence if stn.between(tp, p.start, p.end) => List(p)
            case _ => Nil
          })
          .map(s => (AbstractFluent(s.sv, s.conditionValue), s))
      conditionsAt += ((tp, list))
    }
    conditionsAt(tp)
  }

  def getChangesStartingFrom(tp: AbsTP) = {
    if(!changesFrom.containsKey(tp)) {
      val list : util.List[AbstractLogStatement] = logStatements
        .filter(s => s.hasEffectAtEnd && stn.concurrent(tp, s.start))
      changesFrom += ((tp, list))
    }
    changesFrom(tp)
  }

  /** For every fluent 'p' achieved by this action, gives a list of fluent that are achieved at the same time */
  lazy val concurrentChanges : util.Map[AbstractFluent, util.List[AbstractFluent]] = {
    val map = new util.HashMap[AbstractFluent, util.List[AbstractFluent]]()
    for(s <- jLogStatements if !s.isInstanceOf[AbstractPersistence]) {
      val endValue = s match {
        case t:AbstractTransition => t.effectValue
        case a: AbstractAssignment => a.effectValue
      }
      val list = new util.LinkedList[AbstractFluent]()

      for(s2 <- jLogStatements if s != s2) s2 match {
        case t: AbstractTransition =>
          if(stn.concurrent(s.end, s2.end))
            list.add(AbstractFluent(t.sv, t.effectValue))
        case a: AbstractAssignment =>
          if(stn.concurrent(a.end, s.end))
            list.add(AbstractFluent(a.sv, a.effectValue))
        case p: AbstractPersistence =>
      }

      map.put(AbstractFluent(s.sv, endValue), list)
    }
    map
  }

  override def toString = name
}

/** "timepoint" is rigidly fixed to "anchor": timepoint +delay = anchor */
case class ActAnchoredTimepoint(timepoint: AbsTP, anchor :AbsTP, delay :Int)

object AbstractDuration {

  /** Creates an abstract duration from an expression. */
  def apply(e : Expr, context : AbstractContext, pb : AnmlProblem) : IntExpression = {
    e match {
      case v : NumExpr => IntExpression.lit(v.value.toInt)
      case e : Expr => IntExpression.locSV(StatementsFactory.asStateVariable(e, context, pb))
    }
  }
}

object AbstractAction {

  /** Factory method to build an abstract action
    *
    * @param act Action from the parser to be converted
    * @param pb Problem in which the action is defined
    * @return
    */
  def apply(act:Action, pb:AnmlProblem, refCounter: RefCounter) : List[AbstractAction] = {
    def t(typeName: PType) = pb.instances.asType(typeName)
    try {
      val baseName = act.name
      val taskName = "t-"+baseName
      val args = act.args.map(a => EVariable(a.name, t (a.tipe)))

      val decompositions = act.content.collect{ case x:Decomposition => x }
      val content = act.content.filterNot(_.isInstanceOf[Decomposition])

      val decIdsAndStatements: Seq[(Int, Seq[DecompositionContent])] = decompositions.size match {
        case 0 => List((0, Nil))
        case n => (1 to n).zip(decompositions.map(_.content))
      }

      val acts = for ((decID, additionalStatements) <- decIdsAndStatements) yield {
        val actContext = new PartialContext(pb, Some(pb.context))
        var actChronicle : AbstractChronicle = EmptyAbstractChronicle
        var isTaskDependent = false

        actChronicle = actChronicle.withVariableDeclarations(args)
        args.foreach(arg => actContext.addUndefinedVar(arg))

        val allConstraints = ArrayBuffer[AbstractConstraint]()

        val actionStart = ContainerStart
        val actionEnd = ContainerEnd
        if(decompositions.nonEmpty) {
          allConstraints += new AbstractTimepointType(actionStart, TimepointTypeEnum.DISPATCHABLE_BY_DEFAULT)
          allConstraints += new AbstractTimepointType(actionEnd, TimepointTypeEnum.DISPATCHABLE_BY_DEFAULT)
        }else {
          allConstraints += new AbstractTimepointType(actionStart, TimepointTypeEnum.STRUCTURAL_BY_DEFAULT)
          allConstraints += new AbstractTimepointType(actionEnd, TimepointTypeEnum.STRUCTURAL_BY_DEFAULT)
        }

        allConstraints += new AbstractMinDelay(actionStart, actionEnd, IntExpression.lit(1))


        content foreach {
          case ts: TemporalStatement =>
            val ac = StatementsFactory(ts, actContext, refCounter, DefaultMod)
            actChronicle += ac
          case tempConstraint: TemporalConstraint =>
            actChronicle = actChronicle.withConstraintsSeq(AbstractTemporalConstraint(tempConstraint))
          case Motivated =>
            isTaskDependent = true
          case parser.ExactDuration(e) =>
            val dur = AbstractDuration(e, actContext, pb)
            actChronicle = actChronicle.withConstraintsSeq(AbstractExactDelay(actionStart, actionEnd, dur))
          case parser.UncertainDuration(min, max) =>
            val minDur = AbstractDuration(min, actContext, pb)
            val maxDur = AbstractDuration(max, actContext, pb)
            actChronicle = actChronicle.withConstraints(new AbstractContingentConstraint(actionStart, actionEnd, minDur, maxDur))
          case const: Constant =>
            val v = EVariable(const.name, t(const.tipe))
            actContext.addUndefinedVar(v)
            actChronicle = actChronicle.withVariableDeclarations(v :: Nil)
          case x =>
            throw new ANMLException("DUnsupported block in action: "+x)
        }

        additionalStatements foreach {
          case constraint: TemporalConstraint =>
            actChronicle = actChronicle.withConstraintsSeq(AbstractTemporalConstraint(constraint))
          case const: Constant => // constant function with no arguments is interpreted as local variable
            val v = EVariable(const.name, t(const.tipe))
            actContext.addUndefinedVar(v)
            actChronicle = actChronicle.withVariableDeclarations(v :: Nil)
          case statement: TemporalStatement =>
            val ac = StatementsFactory(statement, actContext, refCounter, DefaultMod)
            actChronicle += ac
          case x => throw new ANMLException("Unsupported statement in decomposition: "+x)
        }
        actChronicle.subTasks.foreach(t =>
          if(!pb.tasksMinDurations.contains(t.name) && t.name != taskName)
            println(s"No known duration of task ${t.name} when processing action $baseName. Assuming a minimal duration of 1 but you should define it earlier.")
        )
        actChronicle = actChronicle.withConstraintsSeq(actChronicle.subTasks.map(t => new AbstractMinDelay(t.start, t.end, IntExpression.lit(pb.tasksMinDurations.getOrElse(t.name, 1)))))

        //TODO: make sure it is replaced
//        for ((function, variable) <- action.context.bindings) {
//          val sv = new AbstractParameterizedStateVariable(function.func, function.args.map(a => action.context.getLocalVar(a.name)))
//          allConstraints += new AbstractEqualityConstraint(sv, action.context.getLocalVar(variable.name), LStatementRef(""))
//        }

        actChronicle = actChronicle.withMinimizedTemporalConstraints(Nil)

        val action = new AbstractAction(baseName, taskName, decID, args, isTaskDependent, actContext, actChronicle)
        action
      }
      assert(!pb.tasksMinDurations.contains(acts.head.taskName))
      pb.tasksMinDurations.put(acts.head.taskName, acts.map(a => a.minDelay(a.start,a.end).lb).min)
      acts.toList
    } catch {
      case e:Throwable =>
        throw new ANMLException(s"Unable to build action \'${act.name}\'", e)
    }
  }
}