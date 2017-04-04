package fr.laas.fape.constraints.meta.stn.constraint

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintSatisfaction, ReversibleConstraint}
import fr.laas.fape.constraints.meta.events.{Event, NewConstraintEvent}
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.variables.IVar

abstract class TemporalConstraint extends Constraint {


 override def _propagate(event: Event)(implicit csp: CSP): Unit = event match {
   case NewConstraintEvent(c) =>
     assert(this == c)
     assert(csp.stn != null)
     csp.stn.addConstraint(this)
   case _ =>
     // nothing, should be handled by underlying STN
 }
}

case class MinDelayConstraint(src:Timepoint, dst:Timepoint, minDelay: Int)
  extends TemporalConstraint with ReversibleConstraint {
  override def toString = s"$src + $minDelay <= $dst"

  override def variables(implicit csp: CSP): Set[IVar] = Set(src, dst)

  override def satisfaction(implicit csp: CSP): Satisfaction =
    if(csp.stn.getMinDelay(src, dst) >= minDelay)
      ConstraintSatisfaction.SATISFIED
    else if(csp.stn.getMaxDelay(src, dst) < minDelay)
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED

  override def reverse: MinDelayConstraint =
    new MinDelayConstraint(dst, src, -minDelay +1)
}

case class ContingentConstraint(src :Timepoint, dst :Timepoint, min :Int, max :Int) extends TemporalConstraint {

  override def variables(implicit csp: CSP): Set[IVar] = Set(src, dst)

  override def toString = s"$src == [$min, $max] ==> $dst"

  override def satisfaction(implicit csp: CSP): Satisfaction = ConstraintSatisfaction.UNDEFINED
}
