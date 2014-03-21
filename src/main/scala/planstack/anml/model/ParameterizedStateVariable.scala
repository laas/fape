package planstack.anml.model

import planstack.anml.{ANMLException, parser}
import scala.collection.JavaConversions._

import planstack.anml.model.concrete.VarRef


class AbstractParameterizedStateVariable(val func:Function, val args:List[LVarRef]) {

  /** Produces a new ParameterizedStateVariable whose parameters refer to global variables (as defined in `context` */
  def bind(context:Context) : ParameterizedStateVariable =
    new ParameterizedStateVariable(func, args.map(context.getGlobalVar(_)))

  def jArgs = seqAsJavaList(args)

  override def toString = "%s(%s)".format(func.name, args.mkString(", "))
}

class ParameterizedStateVariable(val func:Function, val args:List[VarRef]) {

  def jArgs = seqAsJavaList(args)

  override def toString = "%s(%s)".format(func.name, args.mkString(", "))
}

object AbstractParameterizedStateVariable {

  def apply(pb:AnmlProblem, context:AbstractContext, expr:parser.Expr) : AbstractParameterizedStateVariable = {
    val func:parser.FuncExpr = expr match {
      case parser.FuncExpr(nameParts, argList) => {
        if(pb.functions.isDefined(nameParts.mkString("."))) {
          parser.FuncExpr(nameParts, argList)
        } else {
          assert(nameParts.tail.length == 1)
          val headType = context.getType(new LVarRef(nameParts.head))
          parser.FuncExpr(pb.instances.getQualifiedFunction(headType,nameParts.tail.head), parser.VarExpr(nameParts.head)::argList)
        }
      }
      case parser.VarExpr(x) => throw new ANMLException("Unauthorized conversion of VarExpr into a ParameterizedStateVariable: "+expr);
    }
    new AbstractParameterizedStateVariable(pb.functions.get(func.functionName), func.args.map(e => new LVarRef(e.variable)))
  }

}






