package fr.laas.fape.graph

import fr.laas.fape.graph.core.impl.intindexed.DirectedSimpleLabeledIIAdjList
import fr.laas.fape.graph.core.{Graph, LabeledEdge}

class DirectedSimpleLabeledIIAdjListSuite
  extends BaseGraphSuite[Int,Int,LabeledEdge[Int,Int]]
  with SimpleGraphSuite[Int,Int,LabeledEdge[Int,Int]]
  with LabeledGraphSuite[Int, Int]
{
  override val vertices = 0 to 100

  val graph: Graph[Int, Int, LabeledEdge[Int, Int]] = new DirectedSimpleLabeledIIAdjList[Int]()
}
