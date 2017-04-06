package fr.laas.fape.constraints.meta.stn.constraint

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintSatisfaction}
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.variables.IVar

class AbsoluteAfterConstraint(val tp: Timepoint, val deadline: Int)
  extends TemporalConstraint {

  override def satisfaction(implicit csp: CSP): Satisfaction =
    if(tp.domain.lb >= deadline)
      ConstraintSatisfaction.SATISFIED
    else if(tp.domain.ub < deadline)
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED

  override def variables(implicit csp: CSP): Set[IVar] =
    Set(csp.varStore.getDelayVariable(csp.temporalOrigin, tp))

  override def toString = s"$tp >= $deadline"

  override def reverse: AbsoluteBeforeConstraint = tp < deadline
}
