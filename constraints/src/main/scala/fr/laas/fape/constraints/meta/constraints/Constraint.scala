package fr.laas.fape.constraints.meta.constraints


import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.{IVar, Variable}

abstract class Constraint {

  type Satisfaction = ConstraintSatisfaction.ConstraintSatisfaction

  def variables(implicit csp: CSP): Set[IVar]

  final def propagate(event: Event)(implicit csp: CSP): Unit = {
    csp.log.startConstraintPropagation(this)
    _propagate(event)
    csp.log.endConstraintPropagation(this)
  }

  def _propagate(event: Event)(implicit csp: CSP)

  def satisfied(implicit csp: CSP) : Satisfaction

}

trait InversibleConstraint {
  def invert() : Constraint
}

object ConstraintSatisfaction extends Enumeration {
  type ConstraintSatisfaction = Value
  val SATISFIED, UNSATISFIED, UNDEFINED = Value
}