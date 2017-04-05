package fr.laas.fape.constraints.meta.stn.variables

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.domains.IntervalDomain
import fr.laas.fape.constraints.meta.stn.constraint.MinDelayConstraint
import fr.laas.fape.constraints.meta.variables.{IVar, WithDomain}

class TemporalDelay(val from: Timepoint, val to: Timepoint, id: Int) extends IVar(id) with WithDomain {

  override def domain(implicit csp: CSP) : IntervalDomain = csp.dom(this)

  def <=(value: Int) = new MinDelayConstraint(from, to, value)
  def <(value: Int) = this <= value+1

  def >=(value: Int) = new MinDelayConstraint(to, from, -value)
  def >(value: Int) = this >= value-1

  def ==(value: Int) = this <= value && this >= value

  override def toString = s"delay($from, $to)"
}
