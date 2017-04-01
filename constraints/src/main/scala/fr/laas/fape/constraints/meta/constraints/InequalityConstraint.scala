package fr.laas.fape.constraints.meta.constraints

import fr.laas.fape.anml.model.concrete.Variable
import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event

class InequalityConstraint(val v1: Variable, val v2: Variable) extends Constraint {

  override def variables: Set[Variable] = Set(v1, v2)

  override def propagate(event: Event)(implicit csp: CSP) : Unit = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.isSingleton && d2.isSingleton && d1 == d2)
      throw new InconsistentBindingConstraintNetwork()

    else if(d1.isSingleton)
      csp.updateDomain(v2, d2 - d1)

    else if(d2.isSingleton)
      csp.updateDomain(v1, d1 - d2)
  }

  override def satisfied(implicit csp: CSP): Satisfaction = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.emptyIntersection(d2))
      ConstraintSatisfaction.SATISFIED
    else if(d1.isSingleton && d2.isSingleton && d1 == d2)
      ConstraintSatisfaction.UNSATISFIED
    else
      ConstraintSatisfaction.UNDEFINED
  }
}
