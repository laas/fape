package planstack.anml.model.concrete

import planstack.anml.model.ParameterizedStateVariable


abstract class Constraint

abstract class TemporalConstraint extends Constraint {
  def src : TPRef
  def dst : TPRef
  def isParameterized : Boolean
}

case class MinDelayConstraint(src:TPRef, dst:TPRef, minDelay:Integer) extends TemporalConstraint {
  override def toString = s"$src + $minDelay <= $dst"
  override def isParameterized = false
}

case class ParameterizedMinDelayConstraint(src:TPRef, dst:TPRef, minDelay:ParameterizedStateVariable, trans: (Int => Int)) extends TemporalConstraint {
  override def toString = "%s >= %s + f(%s)".format(dst, src, minDelay)
  override def isParameterized = true
}

case class ParameterizedExactDelayConstraint(src:TPRef, dst:TPRef, delay:ParameterizedStateVariable, trans: (Int => Int)) extends TemporalConstraint {
  override def toString = "%s = %s + f(%s)".format(dst, src, delay)
  override def isParameterized = true
}

case class ContingentConstraint(src :TPRef, dst :TPRef, min :Int, max :Int) extends TemporalConstraint {
  override def toString = s"$src == [$min, $max] ==> $dst"
  override def isParameterized = false
}

case class ParameterizedContingentConstraint(src :TPRef, dst :TPRef, min :ParameterizedStateVariable,
                                max :ParameterizedStateVariable, minTrans: (Int => Int), maxTrans: (Int => Int))
  extends TemporalConstraint
{
  override def toString = s"$src == [$min, $max] ==> $dst"
  override def isParameterized = true
}


abstract class BindingConstraint extends Constraint

class AssignmentConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint

class IntegerAssignmentConstraint(val sv : ParameterizedStateVariable, val value : Int) extends BindingConstraint

class VarEqualityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint

class EqualityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint

class VarInequalityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint

class InequalityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint