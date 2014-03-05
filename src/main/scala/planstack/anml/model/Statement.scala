package planstack.anml.model

import planstack.anml.{ANMLException, parser}


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

  def apply(pb:AnmlProblem, expr:parser.Expr) : ParameterizedStateVariable = {
    val func:parser.FuncExpr = expr match {
      case parser.FuncExpr(nameParts, argList) => {
        if(pb.functions.isDefined(nameParts.mkString("."))) {
          parser.FuncExpr(nameParts, argList)
        } else {
          assert(pb.instances.containsInstance(nameParts.head))
          assert(nameParts.tail.length == 1)
          parser.FuncExpr(pb.instances.getQualifiedFunction(pb.instances.typeOf(nameParts.head),nameParts.tail.head), parser.VarExpr(nameParts.head)::argList)
        }
      }
      case parser.VarExpr(x) => throw new ANMLException("Unauthorized conversion of VarExpr into a ParameterizedStateVariable: "+expr);
    }
    new ParameterizedStateVariable(pb.functions.get(func.functionName), func.args.map(_.variable))
  }

}


abstract class AbstractStatement(val sv:ParameterizedStateVariable) {
  def bind(context:Context) : Statement
}

object AbstractStatement {

  def apply(pb:AnmlProblem, statement:parser.Statement) : AbstractStatement = {
    val sv = ParameterizedStateVariable(pb, statement.variable)

    statement match {
      case a:parser.Assignment => new AbstractAssignment(sv, a.right.variable)
      case t:parser.Transition => new AbstractTransition(sv, t.from.variable, t.to.variable)
      case p:parser.Persistence => new AbstractPersistence(sv, p.value.variable)
    }
  }
}

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







abstract class Statement(val context:Context, val sv:ParameterizedStateVariable) extends TemporalInterval {
}

class Assignment(context:Context, sv:ParameterizedStateVariable, val value:String)
  extends Statement(context, sv)
{
  override def toString = "%s := %s".format(sv, value)
}

class Transition(context:Context, sv:ParameterizedStateVariable, val from:String, val to:String)
  extends Statement(context, sv)
{
  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class Persistence(context:Context, sv:ParameterizedStateVariable, val value:String)
  extends Statement(context, sv)
{
  override def toString = "%s == %s".format(sv, value)
}