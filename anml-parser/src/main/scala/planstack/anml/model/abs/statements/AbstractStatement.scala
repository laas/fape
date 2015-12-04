package planstack.anml.model.abs.statements

import planstack.anml.ANMLException
import planstack.anml.model._
import planstack.anml.model.abs.time.{IntervalEnd, IntervalStart, AbsTP, AbstractTemporalAnnotation}
import planstack.anml.model.abs.{AbstractExactDelay, AbstractMaxDelay, AbstractMinDelay}
import planstack.anml.model.concrete.statements._
import planstack.anml.model.concrete.{Chronicle, RefCounter}

abstract trait ChronicleComponent

abstract class AbstractStatement(val id:LocalRef) {
  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  def bind(context:Context, pb:AnmlProblem, container: Chronicle, refCounter: RefCounter) : Any

  def start : AbsTP = IntervalStart(id)
  def end : AbsTP = IntervalEnd(id)

  /** Produces the temporal constraints by applying the temporal annotation to this statement. */
  def getTemporalConstraints(annot : AbstractTemporalAnnotation) : List[AbstractMinDelay] = {
    val stStart = IntervalStart(id)
    val stEnd = IntervalEnd(id)
    (annot, this) match {
      case (AbstractTemporalAnnotation(s, e, "is"), tr:AbstractTransition)  if s == e =>
        throw new ANMLException("Instantaneous transitions are not allowed: "+this)
      case (AbstractTemporalAnnotation(s, e, "is"), ass:AbstractAssignment) =>
        assert(s == e, "Non instantaneous assignment: "+this)
        AbstractExactDelay(annot.start.timepoint, stEnd, annot.start.delta) ++
          AbstractExactDelay(stStart, stEnd, 1)
      case (AbstractTemporalAnnotation(s, e, "is"), _) =>
        AbstractExactDelay(annot.start.timepoint, stStart, annot.start.delta) ++
          AbstractExactDelay(annot.end.timepoint, stEnd, annot.end.delta)
      case ((AbstractTemporalAnnotation(_, _, "contains"), s:AbstractTransition)) =>
        throw new ANMLException("The contains annotation is not allowed on transitions ")
      case ((AbstractTemporalAnnotation(_, _, "contains"), s:AbstractAssignment)) =>
        throw new ANMLException("The contains annotation is not allowed on assignments ")
      case (AbstractTemporalAnnotation(s,e,"contains"), i) => List(
        new AbstractMinDelay(s.timepoint, stStart, s.delta), // start(id) >= start+delta
        new AbstractMinDelay(stEnd, e.timepoint, -e.delta) // end(id) <= end+delta
      )
    }
  }
}

abstract class AbstractLogStatement(val sv:AbstractParameterizedStateVariable, override val id:LStatementRef)
  extends AbstractStatement(id)
{
  require(sv.func.valueType != "integer", "Error: the function of this LogStatement has an integer value.")
  def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) : LogStatement

  def hasConditionAtStart : Boolean
  def hasEffectAfterEnd : Boolean
  def conditionValue : LVarRef
  def effectValue : LVarRef
}

/**
 * Describes an assignment of a state variable to value `statevariable(x, y) := v`
 * @param sv State variable getting the assignment
 * @param value value of the state variable after the assignment
 */
class AbstractAssignment(sv:AbstractParameterizedStateVariable, val value:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) =
    new Assignment(sv.bind(context), context.getGlobalVar(value), container, refCounter)

  override def toString = "%s := %s".format(sv, value)

  override def hasConditionAtStart: Boolean = false
  override def conditionValue: LVarRef = throw new ANMLException("Assignments have conditions at start")
  override def effectValue: LVarRef = value
  override def hasEffectAfterEnd: Boolean = true
}

class AbstractTransition(sv:AbstractParameterizedStateVariable, val from:LVarRef, val to:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) =
    new Transition(sv.bind(context), context.getGlobalVar(from), context.getGlobalVar(to), container, refCounter)

  override def toString = "%s == %s :-> %s".format(sv, from, to)

  override def hasConditionAtStart: Boolean = true
  override def conditionValue: LVarRef = from
  override def effectValue: LVarRef = to
  override def hasEffectAfterEnd: Boolean = true
}

class AbstractPersistence(sv:AbstractParameterizedStateVariable, val value:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) =
    new Persistence(sv.bind(context), context.getGlobalVar(value), container, refCounter)

  override def toString = "%s == %s".format(sv, value)

  override def hasConditionAtStart: Boolean = true
  override def conditionValue: LVarRef = value
  override def effectValue: LVarRef = throw new ANMLException("Persistences have no effects at end")
  override def hasEffectAfterEnd: Boolean = false
}
