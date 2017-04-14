package fr.laas.fape.constraints.meta.constraints

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.domains.BooleanDomain
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.variables.{IVar, ReificationVariable, IntVariable}
import ConstraintSatisfaction._

class ReificationConstraint(val reiVar: ReificationVariable, val constraint: Constraint)
  extends Constraint {
  require(reiVar.constraint == constraint)

  override def variables(implicit csp: CSP): Set[IVar] = Set(reiVar)

  override def subconstraints(implicit csp: CSP) = Set(constraint)

  override def _propagate(event: Event)(implicit csp: CSP): Unit = {
    event match {
      case NewConstraint(c) if c == this =>
        checkVariable
        checkConstraint
      case event: DomainChange =>
        assert(event.variable == reiVar)
        checkVariable
      case WatchedSatisfied(c) =>
        assert(c == constraint)
        csp.updateDomain(reiVar, reiVar.domain - 0)
      case WatchedViolated(c) =>
        assert(c == constraint)
        csp.updateDomain(reiVar, reiVar.domain - 1)
    }
  }

  private def checkVariable(implicit csp: CSP) {
    val dom = csp.dom(reiVar)
    if(dom.isSingleton && dom.contains(1)) {
      csp.post(constraint)
    } else if(dom.isSingleton && dom.contains(0)) {
      csp.post(constraint.reverse)
    } else {
      assert(dom.size == 2, "Reification variable should never have an empty domain")
    }
  }

  private def checkConstraint(implicit csp: CSP) {
    if(constraint.isSatisfied)
      csp.updateDomain(reiVar, new BooleanDomain(Set(true)))
    else if(constraint.isViolated)
      csp.updateDomain(reiVar, new BooleanDomain(Set(false)))
  }

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    val dom = csp.dom(reiVar)
    val cSat = constraint.satisfaction
    if (dom.isSingleton && dom.contains(1)) {
      if (cSat == SATISFIED) SATISFIED
      else if (cSat == UNDEFINED) UNDEFINED
      else VIOLATED
    } else if (dom.isSingleton && dom.contains(0)) {
      if (cSat == SATISFIED) VIOLATED
      else if (cSat == UNDEFINED) UNDEFINED
      else SATISFIED
    } else {
      assert(dom.size == 2, "Reification variable should never have an empty domain")
      UNDEFINED
    }
  }

  override def toString = s"[$reiVar] <=> $constraint"

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint = ???
}
