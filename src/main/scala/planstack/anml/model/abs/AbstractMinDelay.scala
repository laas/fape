package planstack.anml.model.abs

import planstack.anml.model.abs.statements.AbstractStatement
import planstack.anml.model.abs.time.AbsTP
import planstack.anml.model.concrete._
import planstack.anml.model.concrete.time.TimepointRef
import planstack.anml.model._
import planstack.anml.parser

abstract class AbstractConstraint {
  def bind(context: Context, pb :AnmlProblem) : Constraint
}

abstract class AbstractTemporalConstraint extends AbstractConstraint {

  override final def bind(context: Context, pb :AnmlProblem) : TemporalConstraint = this match {
    case AbstractMinDelay(from, to, minDelay) =>
      new MinDelayConstraint(TimepointRef(pb, context, from), TimepointRef(pb, context, to), minDelay)
    case AbstractParameterizedMinDelay(from, to, minDelay, trans) =>
      new ParameterizedMinDelayConstraint(TimepointRef(pb, context, from), TimepointRef(pb, context, to), minDelay.bind(context), trans)
    case AbstractContingentConstraint(from, to, min, max) =>
      new ContingentConstraint(TimepointRef(pb,context,from), TimepointRef(pb,context,to), min, max)
  }
}

case class AbstractMinDelay(from:AbsTP, to:AbsTP, minDelay:Integer)
  extends AbstractTemporalConstraint
{
  override def toString = "%s + %s <= %s".format(from, minDelay, to)
}

case class AbstractParameterizedMinDelay(from :AbsTP, to :AbsTP, minDelay :AbstractParameterizedStateVariable, trans: (Int => Int))
  extends AbstractTemporalConstraint
{
  override def toString = "%s + %s <= %s".format(from, minDelay, to)
}

case class AbstractContingentConstraint(from :AbsTP, to :AbsTP, min :Int, max:Int)
  extends AbstractTemporalConstraint
{
  override def toString = s"$from == [$min, $max] ==> $to"
}

case class AbstractParameterizedContingentConstraint(from :AbsTP,
                                                     to :AbsTP,
                                                     min :ParameterizedStateVariable,
                                                     max:ParameterizedStateVariable,
                                                     minTrans: (Int => Int),
                                                     maxTrans: (Int => Int))
  extends AbstractTemporalConstraint
{
  override def toString = s"$from == [f1($min), f2($max)] ==> $to"
}

object AbstractMaxDelay {
  def apply(from:AbsTP, to:AbsTP, maxDelay:Int) =
    new AbstractMinDelay(to, from, -maxDelay)
}

object AbstractTemporalConstraint {
  private implicit def atr(tp: planstack.anml.parser.TimepointRef) = AbsTP(tp)

  def apply(parsed: parser.TemporalConstraint) : List[AbstractTemporalConstraint] = parsed match {
    case parser.TemporalConstraint(tp1, "=", tp2, d) =>
      List(AbstractMinDelay(tp2,tp1,d), AbstractMaxDelay(tp2,tp1,d))
    case parser.TemporalConstraint(tp1, "<", tp2, d) =>
      List(AbstractMaxDelay(tp2, tp1, d-1))
  }
}

object AbstractMinDelay {

  /** Returns a new Abstract temporal constraint enforcing action1 to be before action2
    *
    * @param action1 local id of an action
    * @param action2 local id of an action
    * @return
    */
  def before(action1:LActRef, action2:LActRef) =
    new AbstractMinDelay(
      new AbsTP("start", action1),
      new AbsTP("end", action2),
      1)
}