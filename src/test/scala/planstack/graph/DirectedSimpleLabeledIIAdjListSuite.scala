package planstack.graph

import planstack.graph.core.{Graph, LabeledEdge}
import planstack.graph.core.impl.intindexed.DirectedSimpleLabeledIIAdjList

class DirectedSimpleLabeledIIAdjListSuite
  extends BaseGraphSuite[Int,Int,LabeledEdge[Int,Int]]
  with SimpleGraphSuite[Int,Int,LabeledEdge[Int,Int]]
  with LabeledGraphSuite[Int, Int]
{
  override val vertices = 0 to 100

  val graph: Graph[Int, Int, LabeledEdge[Int, Int]] = new DirectedSimpleLabeledIIAdjList[Int]()
}
