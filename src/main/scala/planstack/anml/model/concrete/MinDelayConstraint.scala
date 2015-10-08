package planstack.anml.model.concrete

import planstack.anml.model.abs.{AbstractParameterizedMinDelay, AbstractMinDelay, AbstractMinDelay$}
import planstack.anml.model.concrete.time.TimepointRef
import planstack.anml.model.{ParameterizedStateVariable, AnmlProblem, Context}


abstract class Constraint

abstract class TemporalConstraint extends Constraint

case class MinDelayConstraint(src:TPRef, dst:TPRef, minDelay:Integer) extends TemporalConstraint {
  override def toString = "%s >= %s + %s".format(dst, src, minDelay)
}

case class ParameterizedMinDelayConstraint(src:TPRef, dst:TPRef, minDelay:ParameterizedStateVariable, trans: (Int => Int)) extends TemporalConstraint {
  override def toString = "%s >= %s + %s".format(dst, src, minDelay)
}

case class ContingentConstraint(src :TPRef, dst :TPRef, min :Int, max :Int) extends TemporalConstraint {
  override def toString = s"$src == [$min, $max] ==> $dst"
}


case class ParameterizedContingentConstraint(src :TPRef, dst :TPRef, min :ParameterizedStateVariable,
                                max :ParameterizedStateVariable, minTrans: (Int => Int), maxTrans: (Int => Int))
  extends TemporalConstraint
{
  override def toString = s"$src == [$min, $max] ==> $dst"
}