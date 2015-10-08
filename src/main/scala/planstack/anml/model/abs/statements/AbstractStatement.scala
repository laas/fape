package planstack.anml.model.abs.statements

import planstack.anml.ANMLException
import planstack.anml.model._
import planstack.anml.model.abs.time.{IntervalEnd, IntervalStart, AbsTP, AbstractTemporalAnnotation}
import planstack.anml.model.abs.{AbstractMaxDelay, AbstractMinDelay}
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
    annot.flag match {
      case "is" => List(
        AbstractMinDelay(annot.start.timepoint, stStart, annot.start.delta),
        AbstractMaxDelay(annot.start.timepoint, stStart, annot.start.delta),
        AbstractMinDelay(annot.end.timepoint, stEnd, annot.end.delta),
        AbstractMaxDelay(annot.end.timepoint, stEnd, annot.end.delta)
      )
      case "contains" => List(
        // start(id) >= start+delta <=> start(id) +1 > start+delta <=> start < start(id)+1-delta
        new AbstractMinDelay(annot.start.timepoint, stStart, annot.start.delta),
        // end(id) <= end+delta <=> end(id) < end+delta=1
        new AbstractMinDelay(annot.end.timepoint, stEnd, -annot.end.delta)
      )
    }
  }
}

abstract class AbstractLogStatement(val sv:AbstractParameterizedStateVariable, override val id:LStatementRef)
  extends AbstractStatement(id)
{
  require(sv.func.valueType != "integer", "Error: the function of this LogStatement has an integer value.")
  def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) : LogStatement

  def isTemporalInterval = true
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
}

class AbstractTransition(sv:AbstractParameterizedStateVariable, val from:LVarRef, val to:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) =
    new Transition(sv.bind(context), context.getGlobalVar(from), context.getGlobalVar(to), container, refCounter)

  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class AbstractPersistence(sv:AbstractParameterizedStateVariable, val value:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context, pb:AnmlProblem, container:Chronicle, refCounter: RefCounter) =
    new Persistence(sv.bind(context), context.getGlobalVar(value), container, refCounter)

  override def toString = "%s == %s".format(sv, value)
}

