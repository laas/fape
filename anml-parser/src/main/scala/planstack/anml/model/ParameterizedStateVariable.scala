package planstack.anml.model

import planstack.anml.model.concrete.VarRef
import planstack.anml.{ANMLException, parser}

import scala.collection.JavaConversions._


class AbstractParameterizedStateVariable(val func:Function, val args:List[LVarRef]) {

  /** Produces a new ParameterizedStateVariable whose parameters refer to global variables (as defined in `context` */
  def bind(context:Context) : ParameterizedStateVariable =
    new ParameterizedStateVariable(func, args.map(context.getGlobalVar(_)).toArray)

  def jArgs = seqAsJavaList(args)

  /** True if this state variables represents a resource (i.e. has a numeric type) */
  def isResource = func.isInstanceOf[NumFunction]

  override def toString = "%s(%s)".format(func.name, args.mkString(", "))

  override def hashCode() = func.hashCode() + 59 * args.hashCode()
  override def equals(o: Any) : Boolean = o match {
    case sv: AbstractParameterizedStateVariable => func == sv.func && args == sv.args
    case _ => false
  }
}

/** A state variable parameterized with variables.
  *
  * @param func Function on which this state variables applies.
  * @param args A list of variables that are the parameters of the state variable.
  */
class ParameterizedStateVariable(val func:Function, val args:Array[VarRef]) {
  assert(args.length == func.argTypes.length,
    "There is "+args.length+" arguments instead of "+func.argTypes.length+" for the state varaible: "+func)

  def arg(i: Int) : VarRef = args(i)

  override def toString = "%s(%s)".format(func.name, args.mkString(", "))
}

object AbstractParameterizedStateVariable {

  def apply(pb:AnmlProblem, context:AbstractContext, expr:parser.Expr) : AbstractParameterizedStateVariable = {
    // get a normalized version of the expression
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
      case parser.NumExpr(_) => throw new ANMLException("Cannot build a state variable from a numeric expression: "+expr)
    }
    new AbstractParameterizedStateVariable(pb.functions.get(func.functionName), func.args.map(e => new LVarRef(e.variable)))
  }

}






