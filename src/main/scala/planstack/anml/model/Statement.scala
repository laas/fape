package planstack.anml.model

import planstack.anml.{ANMLException, parser}

import planstack.anml.model.concrete.statements._


trait TemporalInterval {
  val start = Timepoint()
  val end = Timepoint()
}

class ParameterizedStateVariable(val func:Function, val args:List[String]) {

  def bind(context:Context) : ParameterizedStateVariable =
    new ParameterizedStateVariable(func, args.map(context.getGlobalVar(_)))

  override def toString = "%s(%s)".format(func.name, args.mkString(", "))
}

object ParameterizedStateVariable {

  def apply(pb:AnmlProblem, context:AbstractContext, expr:parser.Expr) : ParameterizedStateVariable = {
    val func:parser.FuncExpr = expr match {
      case parser.FuncExpr(nameParts, argList) => {
        if(pb.functions.isDefined(nameParts.mkString("."))) {
          parser.FuncExpr(nameParts, argList)
        } else {
          assert(nameParts.tail.length == 1)
          val headType = context.getType(nameParts.head)
          parser.FuncExpr(pb.instances.getQualifiedFunction(headType,nameParts.tail.head), parser.VarExpr(nameParts.head)::argList)
        }
      }
      case parser.VarExpr(x) => throw new ANMLException("Unauthorized conversion of VarExpr into a ParameterizedStateVariable: "+expr);
    }
    new ParameterizedStateVariable(pb.functions.get(func.functionName), func.args.map(_.variable))
  }

}


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
  override def bind(context:Context) = new Assignment(context, sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s := %s".format(sv, value)
}

class AbstractTransition(sv:ParameterizedStateVariable, val from:String, val to:String)
  extends AbstractStatement(sv)
{
  override def bind(context:Context) = new Transition(context, sv.bind(context), context.getGlobalVar(from), context.getGlobalVar(to))

  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class AbstractPersistence(sv:ParameterizedStateVariable, val value:String)
  extends AbstractStatement(sv)
{
  override def bind(context:Context) = new Persistence(context, sv.bind(context), context.getGlobalVar(value))

  override def toString = "%s == %s".format(sv, value)
}




