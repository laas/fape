package fr.laas.fape.constraints.meta.constraints

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.domains.SingletonDomain
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable, VarWithDomain}

class NegBindConstraint(val variable: IntVariable, val value: Int) extends Constraint {

  override def variables(implicit csp: CSP): Set[IVar] = Set(variable)

  override def satisfaction(implicit csp: CSP): Satisfaction =
    if(variable.domain.isSingleton && variable.domain.contains(value))
      ConstraintSatisfaction.VIOLATED
    else if(!variable.domain.contains(value))
      ConstraintSatisfaction.SATISFIED
    else
      ConstraintSatisfaction.UNDEFINED

  override protected def _propagate(event: Event)(implicit csp: CSP) {
    if(variable.domain.contains(value)) {
      if(variable.domain.size >= 2)
        csp.updateDomain(variable, variable.domain - value)
      else
        throw new InconsistentBindingConstraintNetwork()
    }
  }

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: BindConstraint = new BindConstraint(variable, value)

  override def toString = s"$variable =!= $value"
}
