package fr.laas.fape.constraints.meta.constraints


import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, NewConstraint}
import fr.laas.fape.constraints.meta.variables.{IVar, Variable, VariableSeq}

trait EqualityConstraint extends Constraint with ReversibleConstraint {

  def v1: IVar
  def v2: IVar

  override def toString = s"$v1 === $v2"
}

class VariableEqualityConstraint(override val v1: Variable, override val v2: Variable) extends EqualityConstraint {

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

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.emptyIntersection(d2))
      ConstraintSatisfaction.VIOLATED
    else if(d1.isSingleton && d2.isSingleton && d1.values.head == d2.values.head)
      ConstraintSatisfaction.SATISFIED
    else
      ConstraintSatisfaction.UNDEFINED
  }

  override def reverse: VariableInequalityConstraint = new VariableInequalityConstraint(v1, v2)
}


class VariableSeqEqualityConstraint(override val v1: VariableSeq, override val v2: VariableSeq)
  extends ConjunctionConstraint(v1.variables.zip(v2.variables).map(p => p._1 === p._2))
    with EqualityConstraint
{
  require(v1.variables.size == v2.variables.size)

  override def reverse: VariableSeqInequalityConstraint = new VariableSeqInequalityConstraint(v1, v2)
}


