package fr.laas.fape.constraints.meta.stn.constraint

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintSatisfaction, InversibleConstraint}
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.variables.IVar

class AbsoluteBeforeConstraint(val tp: Timepoint, val deadline: Int)
  extends TemporalConstraint with InversibleConstraint {

  override def satisfied(implicit csp: CSP): Satisfaction =
    if(tp.dom.ub <= deadline)
      ConstraintSatisfaction.SATISFIED
    else if(tp.dom.lb > deadline)
      ConstraintSatisfaction.UNSATISFIED
    else
      ConstraintSatisfaction.UNDEFINED

  override def variables(implicit csp: CSP): Set[IVar] =
    Set(csp.varStore.getDelayVariable(csp.temporalOrigin, tp))

  override def toString = s"$tp <= $deadline"

  override def invert(): AbsoluteAfterConstraint = tp > deadline
}
