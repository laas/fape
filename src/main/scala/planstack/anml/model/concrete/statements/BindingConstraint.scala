package planstack.anml.model.concrete.statements

import planstack.anml.model.ParameterizedStateVariable
import planstack.anml.model.concrete.VarRef

abstract class BindingConstraint

class AssignmentConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint

class VarEqualityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint

class EqualityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint

class VarInequalityConstraint(val leftVar : VarRef, val rightVar : VarRef) extends BindingConstraint

class InequalityConstraint(val sv : ParameterizedStateVariable, val variable : VarRef) extends BindingConstraint