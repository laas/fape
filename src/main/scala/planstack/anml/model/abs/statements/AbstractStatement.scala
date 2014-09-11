package planstack.anml.model.abs.statements

import planstack.anml.ANMLException
import planstack.anml.model._
import planstack.anml.model.abs.AbstractTemporalConstraint
import planstack.anml.model.abs.time.{AbstractTemporalAnnotation, AbstractTimepointRef}
import planstack.anml.model.concrete.statements._

abstract class AbstractStatement(val id:LocalRef) {
  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  def bind(context:Context, pb:AnmlProblem) : Any

  def isTemporalInterval : Boolean

  /** Produces the temporal constraints by applying the temporal annotation to this statement. */
  def getTemporalConstraints(annot : AbstractTemporalAnnotation) : List[AbstractTemporalConstraint] = {
    if(!isTemporalInterval)
      throw new ANMLException("This statement cannot be temporally qualified because it has no start/end timepoints: "+this)
    annot.flag match {
      case "is" => List(
        new AbstractTemporalConstraint(new AbstractTimepointRef("start", id), "=", annot.start.timepoint, annot.start.delta),
        new AbstractTemporalConstraint(new AbstractTimepointRef("end", id), "=", annot.end.timepoint, annot.end.delta)
      )
      case contains => List(
        new AbstractTemporalConstraint(annot.start.timepoint, "<", new AbstractTimepointRef("start", id), -annot.start.delta),
        new AbstractTemporalConstraint(new AbstractTimepointRef("end", id), "<", annot.end.timepoint, annot.end.delta)
      )
    }
  }
}

abstract class AbstractLogStatement(val sv:AbstractParameterizedStateVariable, override val id:LStatementRef)
  extends AbstractStatement(id)
{
  require(sv.func.valueType != "integer", "Error: the function of this LogStatement has an integer value.")
  def bind(context:Context, pb:AnmlProblem) : LogStatement

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
  override def bind(context:Context, pb:AnmlProblem) = new Assignment(sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s := %s".format(sv, value)
}

class AbstractTransition(sv:AbstractParameterizedStateVariable, val from:LVarRef, val to:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context, pb:AnmlProblem) = new Transition(sv.bind(context), context.getGlobalVar(from), context.getGlobalVar(to))

  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class AbstractPersistence(sv:AbstractParameterizedStateVariable, val value:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context, pb:AnmlProblem) = new Persistence(sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s == %s".format(sv, value)
}

