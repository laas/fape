package fr.laas.fape.constraints.meta.search

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.variables.Variable

object BinarySearch {

  def search(_csp: CSP) : CSP = {
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
      .collect{case v: Variable => v}.toSet.toSeq
      .filter(_.domain.size > 1)
      .sortBy(_.domain.size)

    // no decision left, success!
    if(variables.isEmpty)
      return csp

    val variable = variables.head
    val value = variable.domain.values.head

    val decision = variable === value

    val left = csp.clone
    left.post(decision)
    val leftSearch = search(left)
    if(leftSearch != null) {
      return leftSearch
    } else {
      val right = csp.clone
      right.post(decision.reverse)
      return search(right)
    }
  }

}
