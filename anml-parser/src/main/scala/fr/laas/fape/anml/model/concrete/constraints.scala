package fr.laas.fape.anml.model.concrete

import fr.laas.fape.anml.model.ParameterizedStateVariable
import fr.laas.fape.anml.pending.IntExpression


abstract class Constraint extends VariableUser

abstract class TemporalConstraint extends Constraint {
  def src : TPRef
  def dst : TPRef
}

case class MinDelayConstraint(src:TPRef, dst:TPRef, minDelay:IntExpression) extends TemporalConstraint {
  def this(src: TPRef, dst: TPRef, minDelay: Int) = this(src, dst, IntExpression.lit(minDelay))
  override def toString = s"$src + $minDelay <= $dst"

  override def usedVariables = Set(src, dst) ++ minDelay.usedVariables
}

case class ContingentConstraint(src :TPRef, dst :TPRef, min :IntExpression, max :IntExpression) extends TemporalConstraint {
  override def toString = s"$src == [$min, $max] ==> $dst"

  override def usedVariables = Set(src, dst) ++ min.usedVariables ++ max.usedVariables
}


abstract class BindingConstraint extends Constraint

class AssignmentConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint {
  override def usedVariables: Set[Variable] = sv.usedVariables + variable
}

class IntegerAssignmentConstraint(val sv : ParameterizedStateVariable, val value : Int) extends BindingConstraint {
  override def usedVariables: Set[Variable] = sv.usedVariables
}

class VarEqualityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint {
  override def usedVariables: Set[Variable] = Set(leftVar, rightVar)
}

class EqualityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint {
  override def usedVariables: Set[Variable] = sv.usedVariables + variable
}

class VarInequalityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint {
  override def usedVariables: Set[Variable] = Set(leftVar, rightVar)
}

class InequalityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint {
  override def usedVariables: Set[Variable] = sv.usedVariables + variable
}

class InConstraint(val leftVar : VarRef, val rightVars: Set[VarRef]) extends BindingConstraint {
  override def usedVariables: Set[Variable] = Set[Variable](leftVar) ++ rightVars
}