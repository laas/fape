package planstack.anml.model

import planstack.anml.{ANMLException, parser}

import planstack.anml.model.concrete.statements._
import planstack.anml.model.abs.AbstractStatement


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






