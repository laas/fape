package fr.laas.fape.constraints.meta.search

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.variables.{IVar, Variable, VarWithDomain}

object BinarySearch {
  var count = 0

  def search(_csp: CSP, optimizeMakespan: Boolean = false) : CSP = {
    count += 1
    implicit val csp = _csp
    try {
      csp.propagate()
    } catch {
      case e:InconsistentBindingConstraintNetwork =>
        return null
    }

    // variables by increasing domain size
    val variables = csp.constraints.active
      .flatMap(_.variables)
      .collect{case v: VarWithDomain => v}.toSet.toSeq
      .filter(_.domain.size > 1)
      .sortBy(_.domain.size)

    // no decision left, success!
    if(variables.isEmpty) {
      println(s"Got solution of makespan: " + csp.makespan)
      return csp
    }

    val variable = variables.head
    val value = variable.domain.values.head

    val decision = variable === value

    val baseLeft = csp.clone
    baseLeft.post(decision)
    val retLeft = search(baseLeft, optimizeMakespan)
    if(retLeft == null) {
      val baseRight = csp.clone
      baseRight.post(decision.reverse)
      return search(baseRight, optimizeMakespan)
    } else if(!optimizeMakespan) {
      return retLeft
    } else {
      val baseRight = csp.clone
      baseRight.post(baseRight.temporalHorizon < retLeft.makespan)
      val retRight = search(baseRight, optimizeMakespan)
      if(retRight != null)
        return retRight
      else
        return retLeft
    }
  }
}
