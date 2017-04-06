package fr.laas.fape.constraints.meta.variables

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain

class ReificationVariable(initialDomain: Domain, val constraint: Constraint)
  extends BooleanVariable(initialDomain, Some(constraint)) {

  override def toString = s"rei($constraint)"
}
