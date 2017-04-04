package fr.laas.fape.constraints.meta.constraints


import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, NewConstraintEvent}
import fr.laas.fape.constraints.meta.variables.{IVar, Variable, VariableSeq}

abstract class EqualityConstraint(val v1: IVar, val v2: IVar) extends Constraint with InversibleConstraint {

  override def toString = s"$v1 === $v2"
}

class VariableEqualityConstraint(override val v1: Variable, override val v2: Variable) extends EqualityConstraint(v1, v2) {

  override def variables(implicit csp: CSP): Set[IVar] = Set(v1, v2)

  override def _propagate(event: Event)(implicit csp: CSP) : Unit = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if (d1.emptyIntersection(d2))
      throw new InconsistentBindingConstraintNetwork()
    else if (d1.isSingleton) {
      csp.updateDomain(v2, d1)
    } else if (d2.isSingleton) {
      csp.updateDomain(v1, d2)
    } else {
      val inter = d1 intersection d2
      csp.updateDomain(v1, inter)
      csp.updateDomain(v2, inter)
    }
  }

  override def satisfied(implicit csp: CSP): Satisfaction = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.emptyIntersection(d2))
      ConstraintSatisfaction.UNSATISFIED
    else if(d1.isSingleton && d2.isSingleton && d1.values.head == d2.values.head)
      ConstraintSatisfaction.SATISFIED
    else
      ConstraintSatisfaction.UNDEFINED
  }

  override def invert(): Constraint = ???
}


class VariableSeqEqualityConstraint(override val v1: VariableSeq, override val v2: VariableSeq)
  extends EqualityConstraint(v1, v2) {
  require(v1.variables.size == v2.variables.size)

  val subConstraints = v1.variables.zip(v2.variables).map(pair => pair._1 === pair._2)

  override def variables(implicit csp: CSP): Set[IVar] = v1.variables.toSet ++ v2.variables + v1 + v2

  override def _propagate(event: Event)(implicit csp: CSP) : Unit = {
    event match {
      case NewConstraintEvent(c) =>
        assert(c == this)
        for(c <- subConstraints)
          csp.post(c)
      case _ => // ignore, handled by subconstraints
    }
  }

  override def satisfied(implicit csp: CSP): Satisfaction = {
    val satisfactions = subConstraints.map(c => c.satisfied)
    if(satisfactions.contains(ConstraintSatisfaction.UNSATISFIED))
      ConstraintSatisfaction.UNSATISFIED
    else if(satisfactions.contains(ConstraintSatisfaction.UNDEFINED))
      ConstraintSatisfaction.UNDEFINED
    else {
      assert(satisfactions.forall(_ == ConstraintSatisfaction.SATISFIED))
      ConstraintSatisfaction.SATISFIED
    }
  }

  override def invert(): Constraint = ???
}


