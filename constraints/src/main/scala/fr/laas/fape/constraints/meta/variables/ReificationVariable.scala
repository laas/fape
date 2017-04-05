package fr.laas.fape.constraints.meta.variables

import fr.laas.fape.constraints.meta.constraints.Constraint

class ReificationVariable(id: Int, val constraint: Constraint) extends BooleanVariable(id, Some(constraint)) {

  override def toString = s"rei($constraint)"
}
