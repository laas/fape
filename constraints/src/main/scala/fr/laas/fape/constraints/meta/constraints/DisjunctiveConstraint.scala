package fr.laas.fape.constraints.meta.constraints
import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.{IVar, ReificationVariable}

class DisjunctiveConstraint(val constraints: Seq[Constraint with ReversibleConstraint]) extends Constraint {

  override def variables(implicit csp: CSP): Set[IVar] =
    constraints.map(c => csp.reified(c)).toSet

  def reificationVars(implicit csp: CSP): Set[ReificationVariable] =
    constraints.map(c => csp.reified(c)).toSet

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    val vars = reificationVars
    if(vars.exists(v => v.isTrue))
      ConstraintSatisfaction.SATISFIED
    else if(vars.forall(v => v.isFalse))
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED
  }


  override protected def _propagate(event: Event)(implicit csp: CSP) {
    val vars = reificationVars
    if(vars.exists(_.isTrue)) {
      // constraint is satisfied
    } else {
      val undecided = vars.filterNot(_.isFalse)
      if(undecided.isEmpty)
        // all sub-constraints are unsatisfied
        throw new InconsistentBindingConstraintNetwork()
      else if(undecided.size == 1)
        // only one satisfiable constraint, force
        csp.bind(undecided.head, 1)
    }
  }
}
