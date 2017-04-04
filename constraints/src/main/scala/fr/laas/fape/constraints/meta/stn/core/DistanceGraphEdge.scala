package fr.laas.fape.constraints.meta.stn.core

import fr.laas.fape.constraints.meta.stn.variables.Timepoint

class DistanceGraphEdge(val from: Timepoint, val to: Timepoint, val value: Int) {
  override def toString = s"(min-delay $to $from ${-value})"
}