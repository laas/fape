package fr.laas.fape.constraints.meta.stn.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.domains.IntervalDomain

class TemporalInterval(val start: Timepoint, val end: Timepoint) {

  override def toString = s"[$start, $end]"

  def duration(implicit csp: CSP) : TemporalDelay =
    csp.varStore.getDelayVariable(start, end)

  def <(o: TemporalInterval) = this.end < o.start
  def <=(o: TemporalInterval) = this.end <= o.start
  def >(o: TemporalInterval) = o < this
  def >=(o: TemporalInterval) = o <= this
}
