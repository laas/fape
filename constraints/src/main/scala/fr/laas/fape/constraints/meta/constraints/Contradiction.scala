package fr.laas.fape.constraints.meta.constraints
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.IVar

/** An always violated constraint */
class Contradiction extends Constraint {

  override def variables(implicit csp: CSP): Set[IVar] = Set()

  override def satisfaction(implicit csp: CSP): Satisfaction = ConstraintSatisfaction.VIOLATED

  override protected def _propagate(event: Event)(implicit csp: CSP) {}

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint = new Tautology
}
