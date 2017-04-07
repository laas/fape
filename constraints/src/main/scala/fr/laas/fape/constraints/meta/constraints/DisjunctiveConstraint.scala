package fr.laas.fape.constraints.meta.constraints
import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.variables._
import ConstraintSatisfaction._
import fr.laas.fape.constraints.meta.domains.{Domain, EnumeratedDomain, SingletonDomain}



class DisjunctiveConstraint(val constraints: Seq[Constraint]) extends Constraint {

  val decisionVar = new IntVariable(new EnumeratedDomain(constraints.indices))

  override def variables(implicit csp: CSP): Set[IVar] = Set(decisionVar)

  override def subconstraints(implicit csp: CSP) = constraints

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    val satisfactions = constraints.map(_.satisfaction)
    if(satisfactions.contains(SATISFIED))
      SATISFIED
    else if(satisfactions.contains(UNDEFINED))
      UNDEFINED
    else
      VIOLATED
  }


  override protected def _propagate(event: Event)(implicit csp: CSP) {
    event match {
      case WatchedSatisfied(c) =>
        assert(c.isSatisfied)
        if(decisionVar.domain.contains(constraints.indexOf(c)))
          csp.updateDomain(decisionVar, new SingletonDomain(constraints.indexOf(c)))
        else
          assert(decisionVar.domain.nonEmpty, "Decision variable has an empty domain even though one of the subconstraints is satisfired")
      case WatchedViolated(c) =>
        assert(c.isViolated)
        if(decisionVar.domain.contains(constraints.indexOf(c)))
          csp.updateDomain(decisionVar, decisionVar.domain - constraints.indexOf(c))
      case DomainReduced(`decisionVar`) =>
        if(decisionVar.isBound) {
          val selectedConstraint = constraints(decisionVar.value)
          csp.postSubConstraint(selectedConstraint, this)
        }
      case NewConstraint(c) =>
    }
  }

  override def toString = constraints.mkString(" || ")

  override def ||(c: Constraint) =
    new DisjunctiveConstraint(c :: constraints.toList)

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint =
    new ConjunctionConstraint(constraints.map(_.reverse))
}
