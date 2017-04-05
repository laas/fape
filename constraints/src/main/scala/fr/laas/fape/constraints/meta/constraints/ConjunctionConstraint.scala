package fr.laas.fape.constraints.meta.constraints
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, NewConstraintEvent}
import fr.laas.fape.constraints.meta.variables.IVar

class ConjunctionConstraint(val constraints: Seq[Constraint]) extends Constraint {

  override def variables(implicit csp: CSP): Set[IVar] = Set()

  override def satisfaction(implicit csp: CSP): Satisfaction =
    if(constraints.forall(_.isSatisfied))
      ConstraintSatisfaction.SATISFIED
    else if(constraints.exists(_.isViolated))
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED

  override protected def _propagate(event: Event)(implicit csp: CSP) {
    event match {
      case NewConstraintEvent(c) => assert(this == c)
        for(c <- constraints)
          csp.post(c)
      case _ =>
    }
  }

  override def toString = constraints.mkString(" && ")
}
