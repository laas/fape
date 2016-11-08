package fr.laas.fape.constraints.stn

import fr.laas.fape.anml.model.concrete.TPRef

class DistanceGraphEdge(val from: TPRef, val to: TPRef, val value: Int) {
  override def toString() = s"(min-delay $to $from ${-value})"
}
