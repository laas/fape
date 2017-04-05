package fr.laas.fape.constraints.meta.constraints


import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.{IVar, Variable}

trait Constraint {

  type Satisfaction = ConstraintSatisfaction.ConstraintSatisfaction

  def variables(implicit csp: CSP): Set[IVar]

  final def propagate(event: Event)(implicit csp: CSP): Unit = {
    csp.log.startConstraintPropagation(this)
    _propagate(event)
    csp.log.endConstraintPropagation(this)
  }

  protected def _propagate(event: Event)(implicit csp: CSP)

  def satisfaction(implicit csp: CSP) : Satisfaction

  def isSatisfied(implicit csp: CSP) = satisfaction == ConstraintSatisfaction.SATISFIED

  def isViolated(implicit csp: CSP) = satisfaction == ConstraintSatisfaction.VIOLATED
}

trait ReversibleConstraint {

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  def reverse : Constraint
}

object ConstraintSatisfaction extends Enumeration {
  type ConstraintSatisfaction = Value
  val SATISFIED, VIOLATED, UNDEFINED = Value
}