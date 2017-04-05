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

  def &&(constraint: Constraint) : ConjunctionConstraint = constraint match {
    case c: ConjunctionConstraint => new ConjunctionConstraint(constraint :: c.constraints.toList)
    case c => new ConjunctionConstraint(List(this, c))
  }

  def ||(constraint: Constraint with ReversibleConstraint) = constraint match {
    case c: DisjunctiveConstraint => new DisjunctiveConstraint(constraint :: c.constraints.toList)
    case c => new DisjunctiveConstraint(List(this.asInstanceOf[Constraint with ReversibleConstraint], c))
  }
}

trait ReversibleConstraint {

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  def reverse : Constraint

}

object ConstraintSatisfaction extends Enumeration {
  type ConstraintSatisfaction = Value
  val SATISFIED, VIOLATED, UNDEFINED = Value
}