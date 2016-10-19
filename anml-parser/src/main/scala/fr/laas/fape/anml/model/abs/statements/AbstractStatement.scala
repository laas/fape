package fr.laas.fape.anml.model.abs.statements

import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.model.{Context, _}
import fr.laas.fape.anml.model.abs.time.{AbsTP, AbstractTemporalAnnotation, IntervalEnd, IntervalStart}
import fr.laas.fape.anml.model.abs.{AbstractExactDelay, AbstractMinDelay}
import fr.laas.fape.anml.model.concrete.statements.{Assignment, LogStatement, Persistence, Transition}
import fr.laas.fape.anml.model.concrete.{Chronicle, RefCounter}
import fr.laas.fape.anml.pending.IntExpression

abstract trait ChronicleComponent

abstract class AbstractStatement(val id:LocalRef) extends VarContainer {
  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
    *
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
        AbstractExactDelay(annot.start.timepoint, stEnd, IntExpression.lit(annot.start.delta)) ++
          AbstractExactDelay(stStart, stEnd, IntExpression.lit(1))
      case (AbstractTemporalAnnotation(s, e, "is"), _) =>
        AbstractExactDelay(annot.start.timepoint, stStart, IntExpression.lit(annot.start.delta)) ++
          AbstractExactDelay(annot.end.timepoint, stEnd, IntExpression.lit(annot.end.delta))
      case ((AbstractTemporalAnnotation(_, _, "contains"), s:AbstractTransition)) =>
        throw new ANMLException("The contains annotation is not allowed on transitions ")
      case ((AbstractTemporalAnnotation(_, _, "contains"), s:AbstractAssignment)) =>
        throw new ANMLException("The contains annotation is not allowed on assignments ")
      case (AbstractTemporalAnnotation(s,e,"contains"), i) => List(
        new AbstractMinDelay(s.timepoint, stStart, IntExpression.lit(s.delta)), // start(id) >= start+delta
        new AbstractMinDelay(stEnd, e.timepoint, IntExpression.lit(-e.delta)) // end(id) <= end+delta
      )
    }
  }
}

abstract class AbstractLogStatement(val sv:AbstractParameterizedStateVariable, override val id:LStatementRef)
  extends AbstractStatement(id)
{
  require(!sv.func.valueType.isNumeric, "Error: the function of this LogStatement has an integer value.")
  require(!sv.func.isConstant, "LogStatement on a constant function")
  def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) : LogStatement

  def hasConditionAtStart : Boolean
  def hasEffectAtEnd: Boolean
  def conditionValue : LVarRef
  def effectValue : LVarRef
}

/**
 * Describes an assignment of a state variable to value `statevariable(x, y) := v`
  *
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
  override def hasEffectAtEnd: Boolean = true

  override def getAllVars: Set[LVarRef] = sv.getAllVars ++ value.getAllVars
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
  override def hasEffectAtEnd: Boolean = true

  override def getAllVars: Set[LVarRef] = sv.getAllVars ++ from.getAllVars ++ to.getAllVars
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
  override def hasEffectAtEnd: Boolean = false

  override def getAllVars: Set[LVarRef] = sv.getAllVars ++ value.getAllVars
}

