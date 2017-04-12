package fr.laas.fape.constraints.meta.stn.variables

import java.util.Objects

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.IntervalDomain
import fr.laas.fape.constraints.meta.stn.constraint.MinDelay
import fr.laas.fape.constraints.meta.variables.{IVar, VarWithDomain}

class TemporalDelay(val from: Timepoint, val to: Timepoint) extends VarWithDomain {

  override def domain(implicit csp: CSP) : IntervalDomain = csp.dom(this)

  def <=(value: Int) = new MinDelay(from, to, value)
  def <(value: Int) = this <= value+1

  def >=(value: Int) = new MinDelay(to, from, -value)
  def >(value: Int) = this >= value-1

  override def ===(value: Int) = this <= value && this >= value

  override def =!=(value: Int): Constraint = this < value || this > value

  override def toString = s"delay($from, $to)"


  override final val hashCode : Int = Objects.hash(from, to)
  override def equals(o:Any) = o match {
    case o: TemporalDelay => from == o.from && to == o.to
    case _ => false
  }

}
