package fr.laas.fape.constraints.meta.constraints

import fr.laas.fape.anml.model.concrete.Variable
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event

abstract class Constraint {

  type Satisfaction = ConstraintSatisfaction.ConstraintSatisfaction

  def variables: Set[Variable]

  def propagate(event: Event)(implicit csp: CSP)

  def satisfied(implicit csp: CSP) : Satisfaction

}

object ConstraintSatisfaction extends Enumeration {
  type ConstraintSatisfaction = Value
  val SATISFIED, UNSATISFIED, UNDEFINED = Value
}