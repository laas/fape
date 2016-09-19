package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.time.AbsTP
import planstack.anml.model.concrete._
import planstack.anml.model.concrete.time.TimepointRef
import planstack.anml.parser
import planstack.anml.pending.{IntExpression, Invert, IntLiteral, IntExpression$}

abstract class AbstractConstraint extends VarContainer {
  def bind(context: Context, pb :AnmlProblem, refCounter: RefCounter) : Constraint
}

abstract class AbstractTemporalConstraint extends AbstractConstraint {

  override final def bind(context: Context, pb :AnmlProblem, refCounter: RefCounter) : TemporalConstraint = {
    val trans = (lvar: LVarRef) => context.getGlobalVar(lvar)
    this match {
      case AbstractMinDelay(from, to, minDelay) =>
        new MinDelayConstraint(TimepointRef(pb, context, from, refCounter), TimepointRef(pb, context, to, refCounter), minDelay.bind(trans))
      case AbstractContingentConstraint(from, to, min, max) =>
        new ContingentConstraint(TimepointRef(pb,context,from, refCounter), TimepointRef(pb,context,to, refCounter), min.bind(trans), max.bind(trans))
    }
  }

  def from : AbsTP
  def to : AbsTP
  def isParameterized : Boolean
}

case class AbstractMinDelay(from:AbsTP, to:AbsTP, minDelay:IntExpression)
  extends AbstractTemporalConstraint
{
  override def toString = "%s + %s <= %s".format(from, minDelay, to)
  override def isParameterized = minDelay.isParameterized

  override def getAllVars: Set[LVarRef] = Set()
}

case class AbstractContingentConstraint(from :AbsTP, to :AbsTP, min :IntExpression, max:IntExpression)
  extends AbstractTemporalConstraint
{
  override def toString = s"$from == [$min, $max] ==> $to"
  override def isParameterized = min.isParameterized || max.isParameterized

  override def getAllVars: Set[LVarRef] = Set()
}


object AbstractMaxDelay {
  def apply(from:AbsTP, to:AbsTP, maxDelay:IntExpression) =
    new AbstractMinDelay(to, from, IntExpression.minus(maxDelay))
}

object AbstractExactDelay {
  def apply(from:AbsTP, to:AbsTP, delay:IntExpression) =
    List(AbstractMinDelay(from, to, delay), AbstractMaxDelay(from,to,delay))
}

object AbstractTemporalConstraint {
  private implicit def atr(tp: planstack.anml.parser.TimepointRef) = AbsTP(tp)

  def minDelay(tp1: AbsTP, tp2: AbsTP, d: Int) = AbstractMinDelay(tp1,tp2, IntExpression.lit(d))
  def maxDelay(tp1: AbsTP, tp2: AbsTP, d: Int) = AbstractMinDelay(tp2,tp1, IntExpression.lit(-d))

  def apply(parsed: parser.TemporalConstraint) : List[AbstractTemporalConstraint] = parsed match {
    case parser.ReqTemporalConstraint(tp1, "=", tp2, d) =>
      List(minDelay(tp2,tp1,d), maxDelay(tp2,tp1,d))
    case parser.ReqTemporalConstraint(tp1, "<", tp2, d) =>
      List(maxDelay(tp2, tp1, d-1))
    case parser.ContingentConstraint(src, dst, min, max) =>
      List(AbstractContingentConstraint(src, dst, IntExpression.lit(min), IntExpression.lit(max)))
  }
}


abstract class AbstractBindingConstraint
  extends AbstractConstraint
{
  override def bind(context: Context, pb: AnmlProblem, refCounter: RefCounter): BindingConstraint = bind(context, pb)

  def bind(context: Context, pb: AnmlProblem) : BindingConstraint
}

class AbstractAssignmentConstraint(val sv : AbstractParameterizedStateVariable, val variable : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint
{
  require(sv.func.isConstant)

  override def toString = "%s := %s".format(sv, variable)

  override def bind(context: Context, pb: AnmlProblem) =
    new AssignmentConstraint(sv.bind(context), context.getGlobalVar(variable))

  override def getAllVars: Set[LVarRef] = sv.getAllVars ++ variable.getAllVars
}

class AbstractIntAssignmentConstraint(val sv : AbstractParameterizedStateVariable, val value : Int, id:LStatementRef)
  extends AbstractBindingConstraint
{
  require(sv.func.isConstant && sv.func.valueType.isNumeric)

  override def toString = "%s := %s".format(sv, value)

  override def bind(context: Context, pb: AnmlProblem) =
    new IntegerAssignmentConstraint(sv.bind(context), value)

  override def getAllVars: Set[LVarRef] = sv.getAllVars
}

class AbstractEqualityConstraint(val sv : AbstractParameterizedStateVariable, val variable : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint
{
  require(sv.func.isConstant)

  override def toString = "%s == %s".format(sv, variable)

  override def bind(context: Context, pb: AnmlProblem) =
    new EqualityConstraint(sv.bind(context), context.getGlobalVar(variable))

  override def getAllVars: Set[LVarRef] = sv.getAllVars ++ variable.getAllVars
}

class AbstractVarEqualityConstraint(val leftVar : LVarRef, val rightVar : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint
{
  override def toString = "%s == %s".format(leftVar, rightVar)

  override def bind(context: Context, pb: AnmlProblem) =
    new VarEqualityConstraint(context.getGlobalVar(leftVar), context.getGlobalVar(rightVar))

  override def getAllVars: Set[LVarRef] = leftVar.getAllVars ++ rightVar.getAllVars
}

class AbstractInequalityConstraint(val sv : AbstractParameterizedStateVariable, val variable : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint
{
  require(sv.func.isConstant)

  override def toString = "%s != %s".format(sv, variable)

  override def bind(context: Context, pb: AnmlProblem) =
    new InequalityConstraint(sv.bind(context), context.getGlobalVar(variable))

  override def getAllVars: Set[LVarRef] = sv.getAllVars ++ variable.getAllVars
}

class AbstractVarInequalityConstraint(val leftVar : LVarRef, val rightVar : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint
{
  override def toString = "%s != %s".format(leftVar, rightVar)

  override def bind(context: Context, pb: AnmlProblem) =
    new VarInequalityConstraint(context.getGlobalVar(leftVar), context.getGlobalVar(rightVar))

  override def getAllVars: Set[LVarRef] = leftVar.getAllVars ++ rightVar.getAllVars
}

class AbstractInConstraint(val leftVar : LVarRef, val rightVars : Set[LVarRef], id:LStatementRef)
  extends AbstractBindingConstraint
{
  override def toString = "%s in %s".format(leftVar, rightVars)

  override def bind(context: Context, pb: AnmlProblem) =
    new InConstraint(context.getGlobalVar(leftVar), rightVars.map(v => context.getGlobalVar(v)))

  override def getAllVars: Set[LVarRef] = leftVar.getAllVars ++ rightVars.flatMap(a => a.getAllVars)
}