package fr.laas.fape.planning.causality.support

import fr.laas.fape.anml.model.concrete.Action
import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintSatisfaction}
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.IVar
import fr.laas.fape.planning.causality.{DecisionPending, SupportByExistingChange}

/** Enforces that the given support variable is supported by a changing from the given action. */
class SupportByAction(act: Action, supportVar: SupportVar) extends Constraint {
  override def variables(implicit csp: CSP): Set[IVar] = Set(supportVar)

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    if(supportVar.dom.contains(DecisionPending))
      ConstraintSatisfaction.UNDEFINED
    else if(supportVar.dom.values.collect{ case x: SupportByExistingChange => x.c}.exists(_.ref.container == act.chronicle))
      ConstraintSatisfaction.SATISFIED
    else {
      val x = supportVar.dom.values.head
      ConstraintSatisfaction.UNDEFINED
    }
  }

  override protected def _propagate(event: Event)(implicit csp: CSP): Unit = {
    if(supportVar.domain.isEmpty)
      throw new InconsistentBindingConstraintNetwork()
    for((v, i) <- supportVar.dom.valuesWithIntRepresentation) {
      v match {
        case SupportByExistingChange(change) if change.ref.container == act.chronicle =>
        case DecisionPending =>
        case _ =>
          // this value is not valid: either a support by action or a support by existing change which is not in the targeted action
          csp.post(supportVar =!= i)
      }
    }
  }

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint = ???

  override def toString : String = s"SupportByAction($act, $supportVar)"
}
