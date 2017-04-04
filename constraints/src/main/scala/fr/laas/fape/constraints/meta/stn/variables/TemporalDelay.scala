package fr.laas.fape.constraints.meta.stn.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.domains.IntervalDomain
import fr.laas.fape.constraints.meta.variables.IVar

class TemporalDelay(val from: Timepoint, val to: Timepoint, id: Int) extends IVar(id) {

  def dom(implicit csp: CSP) : IntervalDomain = csp.dom(this)
}
