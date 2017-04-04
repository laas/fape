package fr.laas.fape.constraints.meta.stn.constraint

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.ConstraintSatisfaction
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.variables.IVar

class AbsoluteAfterConstraint(val tp: Timepoint, val deadline: Int) extends TemporalConstraint {

  override def satisfied(implicit csp: CSP): Satisfaction =
    if(tp.dom.lb >= deadline)
      ConstraintSatisfaction.SATISFIED
    else if(tp.dom.ub < deadline)
      ConstraintSatisfaction.UNSATISFIED
    else
      ConstraintSatisfaction.UNDEFINED

  override def variables(implicit csp: CSP): Set[IVar] = Set(tp, csp.temporalOrigin)

  override def toString = s"$tp >= $deadline"
}
