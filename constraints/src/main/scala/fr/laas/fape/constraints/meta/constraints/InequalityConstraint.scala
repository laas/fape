package fr.laas.fape.constraints.meta.constraints

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, NewConstraint, Satisfied}
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable, VariableSeq}


trait InequalityConstraint extends Constraint {

  def v1: IVar
  def v2: IVar

  override def toString = s"$v1 =!= $v2"
}

class VariableInequalityConstraint(override val v1: IntVariable, override val v2: IntVariable)
  extends InequalityConstraint {

  override def variables(implicit csp: CSP): Set[IVar] = Set(v1, v2)

  override def _propagate(event: Event)(implicit csp: CSP) : Unit = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.isSingleton && d2.isSingleton && d1 == d2) {
        throw new InconsistentBindingConstraintNetwork()
    } else if(d1.isSingleton) {
      csp.updateDomain(v2, d2 - d1)
    } else if(d2.isSingleton) {
      csp.updateDomain(v1, d1 - d2)
    }
  }

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    val d1 = csp.dom(v1)
    val d2 = csp.dom(v2)

    if(d1.emptyIntersection(d2))
      ConstraintSatisfaction.SATISFIED
    else if(d1.isSingleton && d2.isSingleton && d1 == d2)
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED
  }

  override def reverse: Constraint = new VariableEqualityConstraint(v1, v2)
}

class VariableSeqInequalityConstraint(override val v1: VariableSeq, override val v2: VariableSeq)
  extends DisjunctiveConstraint(v1.variables.zip(v2.variables).map(p => p._1 =!= p._2))
    with InequalityConstraint {

  require(v1.variables.size == v2.variables.size)

  override def reverse: Constraint = new VariableSeqEqualityConstraint(v1, v2)
}