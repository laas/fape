package planstack.anml.model.abs.statements

import planstack.anml.model._
import planstack.anml.model.concrete.statements._
import planstack.anml.parser
import planstack.anml.model.abs.AbstractTemporalConstraint

abstract class AbstractStatement(val sv:AbstractParameterizedStateVariable, val id:LStatementRef) {
  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  def bind(context:Context) : Statement
}

object AbstractStatement {
  def apply(pb:AnmlProblem, context:AbstractContext, statement:parser.Statement) : AbstractStatement = {
    val sv = AbstractParameterizedStateVariable(pb, context, statement.variable)
    val id =
      if(statement.id.isEmpty) new LStatementRef()
      else new LStatementRef(statement.id)

    statement match {
      case a :parser.Assignment => new AbstractAssignment(sv, new LVarRef(a.right.variable), id)
      case t :parser.Transition => new AbstractTransition(sv, new LVarRef(t.from.variable), new LVarRef(t.to.variable), id)
      case p :parser.Persistence => new AbstractPersistence(sv, new LVarRef(p.value.variable), id)
      case set :parser.SetResource => new AbstractSetResource(sv, set.right, id)
      case use :parser.UseResource => new AbstractUseResource(sv, use.right, id)
      case consume :parser.ConsumeResource => new AbstractConsumeResource(sv, consume.right, id)
      case produce :parser.ProduceResource => new AbstractProduceResource(sv, produce.right, id)
      case lend :parser.LendResource => new AbstractLendResource(sv, lend.right, id)
      case require :parser.RequireResource => new AbstractRequireResource(sv, require.operator, require.right, id)
    }
  }
}

abstract class AbstractLogStatement(sv:AbstractParameterizedStateVariable, id:LStatementRef) extends AbstractStatement(sv, id) {
  require(sv.func.valueType != "integer", "Error: the function of this LogStatement has an integer value.")
  def bind(context:Context) : LogStatement
}

/**
 * Describes an assignment of a state variable to value `statevariable(x, y) := v`
 * @param sv State variable getting the assignment
 * @param value value of the state variable after the assignment
 */
class AbstractAssignment(sv:AbstractParameterizedStateVariable, val value:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context) = new Assignment(sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s := %s".format(sv, value)
}

class AbstractTransition(sv:AbstractParameterizedStateVariable, val from:LVarRef, val to:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context) = new Transition(sv.bind(context), context.getGlobalVar(from), context.getGlobalVar(to))

  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class AbstractPersistence(sv:AbstractParameterizedStateVariable, val value:LVarRef, id:LStatementRef)
  extends AbstractLogStatement(sv, id)
{
  override def bind(context:Context) = new Persistence(sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s == %s".format(sv, value)
}

