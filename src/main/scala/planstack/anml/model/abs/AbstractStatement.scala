package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.concrete.statements._
import planstack.anml.parser

abstract class AbstractStatement(val sv:ParameterizedStateVariable) {
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
    val sv = ParameterizedStateVariable(pb, context, statement.variable)

    statement match {
      case a:parser.Assignment => new AbstractAssignment(sv, a.right.variable)
      case t:parser.Transition => new AbstractTransition(sv, t.from.variable, t.to.variable)
      case p:parser.Persistence => new AbstractPersistence(sv, p.value.variable)
    }
  }
}

/**
 * Describes an assignment of a state variable to value `statevariable(x, y) := v`
 * @param sv State variable getting the assignment
 * @param value value of the state variable after the assignment
 */
class AbstractAssignment(sv:ParameterizedStateVariable, val value:String)
  extends AbstractStatement(sv)
{
  override def bind(context:Context) = new Assignment(sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s := %s".format(sv, value)
}

class AbstractTransition(sv:ParameterizedStateVariable, val from:String, val to:String)
  extends AbstractStatement(sv)
{
  override def bind(context:Context) = new Transition(sv.bind(context), context.getGlobalVar(from), context.getGlobalVar(to))

  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class AbstractPersistence(sv:ParameterizedStateVariable, val value:String)
  extends AbstractStatement(sv)
{
  override def bind(context:Context) = new Persistence(sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s == %s".format(sv, value)
}

