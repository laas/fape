package fr.laas.fape.constraints.meta.constraints

import fr.laas.fape.anml.model.concrete.Variable
import fr.laas.fape.constraints.bindings.{InconsistentBindingConstraintNetwork}
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event

class EqualityConstraint(val v1: Variable, val v2: Variable) extends Constraint {

  override def variables: Set[Variable] = Set(v1, v2)

  override def propagate(event: Event)(implicit csp: CSP) : Unit = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(!d1.nonEmptyIntersection(d2))
      throw new InconsistentBindingConstraintNetwork()
    else if(d1.isSingleton) {
      csp.updateDomain(v2, d1)
    } else if(d2.isSingleton) {
      csp.updateDomain(v1, d2)
    } else {
      val inter = d1 intersect d2
      csp.updateDomain(v1, inter)
      csp.updateDomain(v2, inter)
    }
  }

  override def satisfied(implicit csp: CSP): Satisfaction = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.nonEmptyIntersection(d2))
      ConstraintSatisfaction.UNSATISFIED
    else if(d1.isSingleton && d2.isSingleton && d1 == d2)
      ConstraintSatisfaction.SATISFIED
    else
      ConstraintSatisfaction.UNDEFINED
  }
}
