package fr.laas.fape.constraints.meta.constraints

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.{IVar, Variable, VariableSeq}


abstract class InequalityConstraint(val v1: IVar, val v2: IVar) extends Constraint with InversibleConstraint {

  override def toString = s"$v1 =!= $v2"
}

class VariableInequalityConstraint(override val v1: Variable, override val v2: Variable)
  extends InequalityConstraint(v1, v2) {

  override def variables(implicit csp: CSP): Set[IVar] = Set(v1, v2)

  override def _propagate(event: Event)(implicit csp: CSP) : Unit = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.isSingleton && d2.isSingleton)
      if (d1 == d2)
        throw new InconsistentBindingConstraintNetwork()
      else
        csp.setSatisfied(this)

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

  override def invert(): Constraint = new VariableEqualityConstraint(v1, v2)
}

class VariableSeqInequalityConstraint(override val v1: VariableSeq, override val v2: VariableSeq)
  extends InequalityConstraint(v1, v2) {
  require(v1.variables.size == v2.variables.size)

  val subConstraints = v1.variables.zip(v2.variables).map(pair => pair._1 =!= pair._2)

  private def reificationVariables(implicit csp: CSP) = subConstraints.map(c => csp.reified(c))

  override def variables(implicit csp: CSP): Set[IVar] = reificationVariables.toSet

  override def _propagate(event: Event)(implicit csp: CSP): Unit = {
    if(reificationVariables.exists(_.isTrue)) {
      // constraint is satisfied
    } else {
      val undecided = reificationVariables.filterNot(_.isFalse)
      if(undecided.isEmpty)
        // all sub-constraints are unsatisfied
        throw new InconsistentBindingConstraintNetwork()
      else if(undecided.size == 1)
        // only one satisfiable constraint, force
        csp.bind(undecided.head, 1)

    }
  }

  override def satisfied(implicit csp: CSP): Satisfaction = {
    val vars = reificationVariables
    if(vars.exists(v => v.isTrue))
      ConstraintSatisfaction.SATISFIED
    else if(vars.forall(v => v.isFalse))
      ConstraintSatisfaction.UNSATISFIED
    else
      ConstraintSatisfaction.UNDEFINED
  }

  override def invert(): Constraint = new VariableSeqEqualityConstraint(v1, v2)
}