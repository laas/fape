package fr.laas.fape.constraints.meta.constraints
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, NewConstraint}
import fr.laas.fape.constraints.meta.variables.IVar

class ConjunctionConstraint(val constraints: Seq[Constraint]) extends Constraint {

  override def onPost(implicit csp: CSP) {
    for(c <- constraints) {
      csp.postSubConstraint(c, this)
    }
    super.onPost
  }

  override def variables(implicit csp: CSP): Set[IVar] = Set()

  override def subconstraints(implicit csp: CSP) = constraints

  override def satisfaction(implicit csp: CSP): Satisfaction =
    if(constraints.forall(_.isSatisfied))
      ConstraintSatisfaction.SATISFIED
    else if(constraints.exists(_.isViolated))
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED

  override protected def _propagate(event: Event)(implicit csp: CSP) {
  }

  override def toString = constraints.mkString(" && ")

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint =
    new DisjunctiveConstraint(constraints.map(_.reverse))
}
