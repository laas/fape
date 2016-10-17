package fr.laas.fape.anml.model.concrete

import fr.laas.fape.anml.model.ParameterizedStateVariable
import fr.laas.fape.anml.pending.IntExpression


abstract class Constraint

abstract class TemporalConstraint extends Constraint {
  def src : TPRef
  def dst : TPRef
  def isParameterized : Boolean
}

case class MinDelayConstraint(src:TPRef, dst:TPRef, minDelay:IntExpression) extends TemporalConstraint {
  override def toString = s"$src + $minDelay <= $dst"
  override def isParameterized = false
}

case class ContingentConstraint(src :TPRef, dst :TPRef, min :IntExpression, max :IntExpression) extends TemporalConstraint {
  override def toString = s"$src == [$min, $max] ==> $dst"
  override def isParameterized = false
}


abstract class BindingConstraint extends Constraint

case class AssignmentConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint

class IntegerAssignmentConstraint(val sv : ParameterizedStateVariable, val value : Int) extends BindingConstraint

class VarEqualityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint

class EqualityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint

class VarInequalityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint

class InequalityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint

class InConstraint(val leftVar : VarRef, val rightVars: Set[VarRef]) extends BindingConstraint