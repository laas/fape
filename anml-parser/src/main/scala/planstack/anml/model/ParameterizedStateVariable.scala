package planstack.anml.model

import java.util

import planstack.anml.model.abs.DefaultMod
import planstack.anml.model.concrete.VarRef
import planstack.anml.parser.VarExpr
import planstack.anml.{ANMLException, parser}

import scala.collection.JavaConversions._


class AbstractParameterizedStateVariable(val func:Function, val args:List[LVarRef]) extends VarContainer {

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

  override def getAllVars: Set[LVarRef] = args.flatMap(a => a.getAllVars).toSet
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
  override lazy val hashCode = func.hashCode() + 59 * util.Arrays.asList(args).hashCode()
  override def equals(o: Any) = o match {
    case sv: ParameterizedStateVariable =>
      func == sv.func && args.indices.forall(i => args(i) == sv.args(i))
    case _ => false
  }
}

object AbstractParameterizedStateVariable {

  def apply(pb:AnmlProblem, context:AbstractContext, expr:parser.Expr) : AbstractParameterizedStateVariable = {
    context.simplify(expr,DefaultMod) match {
      case f: EFunction =>
        new AbstractParameterizedStateVariable(f.func, f.args)
      case x =>
        throw new ANMLException("Cannot build a state variable from: " + x)
    }
  }
}






