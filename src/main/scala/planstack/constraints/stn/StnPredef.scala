package planstack.constraints.stn

import planstack.graph.core
import planstack.graph.core.LabeledDigraph
import planstack.graph.Predef._

object StnPredef {

  def NewGraph() : LabeledDigraph[Int,Int] = new core.impl.intindexed.DirectedMultiLabeledIIAdjList[Int] //newSimpleLabeledDigraph[Int, Int]

}
