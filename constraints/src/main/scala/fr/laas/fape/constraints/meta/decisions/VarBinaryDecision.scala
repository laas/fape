package fr.laas.fape.constraints.meta.decisions

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.variables.{IntVar, IntVariable, VarWithDomain}

class VarBinaryDecision(v: VarWithDomain) extends Decision {

  override def pending(implicit csp: CSP): Boolean = !v.domain.isSingleton

  override def options(implicit csp: CSP): Seq[DecisionOption] = {
    if(v.domain.isEmpty) {
      List()
    } else {
      val value = v.domain.values.head
      List(new DecisionConstraint(v === value), new DecisionConstraint(v =!= value))
    }
  }

  override def numOption(implicit csp: CSP): Int = v.domain.size
}
