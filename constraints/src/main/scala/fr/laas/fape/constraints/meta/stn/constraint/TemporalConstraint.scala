package fr.laas.fape.constraints.meta.stn.constraint

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintSatisfaction}
import fr.laas.fape.constraints.meta.events.{Event, NewConstraint}
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.variables.IVar

abstract class TemporalConstraint extends Constraint {

  override def onPost(implicit csp: CSP) {
    csp.stn.addConstraint(this)
    super.onPost
  }

 override def _propagate(event: Event)(implicit csp: CSP) {
   // handled by the STN
 }
}

class MinDelay(val src:Timepoint, val dst:Timepoint, val minDelay: Int)
  extends TemporalConstraint {
  override def toString = s"$src + $minDelay <= $dst"

  override def variables(implicit csp: CSP): Set[IVar] =
    Set(csp.varStore.getDelayVariable(src, dst))

  override def satisfaction(implicit csp: CSP): Satisfaction =
    if(csp.stn.getMinDelay(src, dst) >= minDelay)
      ConstraintSatisfaction.SATISFIED
    else if(csp.stn.getMaxDelay(src, dst) < minDelay)
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED

  override def reverse: MinDelay =
    new MinDelay(dst, src, -minDelay +1)
}

class Contingent(val src :Timepoint, val dst :Timepoint, val min :Int, val max :Int) extends TemporalConstraint {

  override def variables(implicit csp: CSP): Set[IVar] = Set(src, dst)

  override def toString = s"$src == [$min, $max] ==> $dst"

  override def satisfaction(implicit csp: CSP): Satisfaction = ConstraintSatisfaction.UNDEFINED

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint = ???
}
