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

/** A state variable parameterized with variables.
  *
  * @param func Function on which this state variables applies.
  * @param args A list of variables that are the parameters of the state variable.
  */
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
          assert(nameParts.tail.length == 1, "Does not seem to be a valid function: "+expr)
          val headType = context.getType(new LVarRef(nameParts.head))
          parser.FuncExpr(pb.instances.getQualifiedFunction(headType,nameParts.tail.head), parser.VarExpr(nameParts.head)::argList)
        }
      }
      case parser.VarExpr(x) => {
        if(pb.functions.isDefined(x)) {
          // it is a function with no arguments
          parser.FuncExpr(List(x), Nil)
        } else {
          throw new ANMLException("This VarExpr does not refer to any existing function: "+expr)
        }
      }
    }
    new AbstractParameterizedStateVariable(pb.functions.get(func.functionName), func.args.map(e => new LVarRef(e.variable)))
  }

}






