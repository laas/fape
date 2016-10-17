package fr.laas.fape.anml.model.abs

import fr.laas.fape.anml
import fr.laas.fape.anml.parser
import fr.laas.fape.anml.model._
import fr.laas.fape.anml.model.abs.time.{AbsTP, TimepointTypeEnum}
import fr.laas.fape.anml.model.concrete.time.TimepointRef
import fr.laas.fape.anml.parser.TimepointRef
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.anml.pending.IntExpression

abstract class AbstractConstraint extends VarContainer {
  def bind(context: Context, pb :AnmlProblem, refCounter: RefCounter) : List[Constraint]
}

abstract class AbstractTemporalConstraint extends AbstractConstraint {
  def bind(context: Context, pb :AnmlProblem, refCounter: RefCounter) : List[TemporalConstraint]
  def timepoints : Set[AbsTP]
  def isParameterized : Boolean
}

case class AbstractTimepointType(tp:AbsTP, genre: TimepointTypeEnum) extends AbstractTemporalConstraint {
  override def bind(context: Context, pb :AnmlProblem, refCounter: RefCounter) : List[TemporalConstraint] = {
    val concreteTP = TimepointRef(pb, context, tp, refCounter)
    concreteTP.genre.setType(genre)
    Nil
  }
  def isParameterized = false
  override def timepoints: Set[AbsTP] = Set(tp)
  override def getAllVars: Set[LVarRef] = Set()
}

case class AbstractMinDelay(from:AbsTP, to:AbsTP, minDelay:IntExpression)
  extends AbstractTemporalConstraint
{
  def this(from: AbsTP, to: AbsTP, minDelay: Int) = this(from, to, IntExpression.lit(minDelay))
  override def toString = "%s + %s <= %s".format(from, minDelay, to)
  override def isParameterized = minDelay.isParameterized

  override def getAllVars: Set[LVarRef] = Set()
  override def timepoints: Set[AbsTP] = Set(from,to)

  override def bind(context: Context, pb :AnmlProblem, refCounter: RefCounter) : List[TemporalConstraint] = {
    val trans = (lvar: LVarRef) => context.getGlobalVar(lvar)
    val fromConcrete = TimepointRef(pb, context, from, refCounter)
    val toConcrete = TimepointRef(pb, context, to, refCounter)
    new MinDelayConstraint(fromConcrete, toConcrete, minDelay.bind(trans)) :: Nil
  }
}

case class AbstractContingentConstraint(from :AbsTP, to :AbsTP, min :IntExpression, max:IntExpression)
  extends AbstractTemporalConstraint
{
  override def toString = s"$from == [$min, $max] ==> $to"
  override def isParameterized = min.isParameterized || max.isParameterized

  override def getAllVars: Set[LVarRef] = Set()
  override def timepoints: Set[AbsTP] = Set(from,to)

  override def bind(context: Context, pb :AnmlProblem, refCounter: RefCounter) : List[TemporalConstraint] = {
    val trans = (lvar: LVarRef) => context.getGlobalVar(lvar)
    val fromConcrete = TimepointRef(pb, context, from, refCounter)
    val toConcrete = TimepointRef(pb, context, to, refCounter)
    assert(!fromConcrete.genre.isNecessarilyStructural, "Structural timepoint at the source of a contingent link")
    fromConcrete.genre.setType(TimepointTypeEnum.DISPATCHABLE_BY_DEFAULT)
    toConcrete.genre.setType(TimepointTypeEnum.CONTINGENT)
    new ContingentConstraint(fromConcrete, toConcrete, min.bind(trans), max.bind(trans)) :: Nil
  }
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
  private implicit def atr(tp: TimepointRef) = AbsTP(tp)

  def minDelay(tp1: AbsTP, tp2: AbsTP, d: Int) = AbstractMinDelay(tp1,tp2, IntExpression.lit(d))
  def maxDelay(tp1: AbsTP, tp2: AbsTP, d: Int) = AbstractMinDelay(tp2,tp1, IntExpression.lit(-d))

  def apply(parsed: anml.parser.TemporalConstraint) : List[AbstractTemporalConstraint] = parsed match {
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
  override def bind(context: Context, pb: AnmlProblem, refCounter: RefCounter): List[BindingConstraint]= bind(context, pb) :: Nil

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