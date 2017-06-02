package fr.laas.fape.constraints.meta.decisions

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.Constraint

case class DecisionConstraint(constraint: Constraint) extends DecisionOption {
  override def enforceIn(csp: CSP): Unit = csp.post(constraint)
}
