package fr.laas.fape.constraints.meta.search

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable, VarWithDomain}

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
    val decisions = csp.decisions.pending
      .filter(_.pending)
      .sortBy(_.numOption)

    // no decision left, success!
    if(decisions.isEmpty) {
      println(s"Got solution of makespan: " + csp.makespan)
      return csp
    }

    val decision = decisions.head

    val base: CSP = csp.clone
    var res: CSP = null

    for(opt <- decision.options) {
      val cloned = base.clone
      opt.enforceIn(cloned)
      val tmp = search(cloned, optimizeMakespan)
      if(tmp != null && !optimizeMakespan) {
        return tmp
      } else if(tmp != null) {
        res = tmp
        base.post(base.temporalHorizon < res.makespan)
      }
    }
    return res
  }
}
