package planstack.constraints.stn

import planstack.graph.Predef._

object StnPredef {

  type G = DirectedSimpleLabeledGraph[Int, Int]
  def NewGraph() : G = NewDirectedSimpleLabeledGraph[Int, Int]

}
