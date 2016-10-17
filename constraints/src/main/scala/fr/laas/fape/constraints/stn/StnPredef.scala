package fr.laas.fape.constraints.stn

import planstack.graph.core
import planstack.graph.core.LabeledDigraph

object StnPredef {

  def NewGraph() : LabeledDigraph[Int,Int] = new core.impl.intindexed.DirectedMultiLabeledIIAdjList[Int]

}
