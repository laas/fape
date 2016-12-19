package fr.laas.fape.graph

import fr.laas.fape.graph.core.impl.{MultiLabeledDirectedAdjacencyList, MultiLabeledUndirectedAdjacencyList, SimpleLabeledDirectedAdjacencyList}
import fr.laas.fape.graph.core.{LabeledEdge, SimpleLabeledDigraph}


class SimpleLabeledAdjListSuite extends LabeledGraphSuite[Int, Int] with SimpleGraphSuite[Int, Int, LabeledEdge[Int, Int]]{
  val graph = new SimpleLabeledDirectedAdjacencyList[Int,Int]()

  test("CompleteType") {
    assert(graph.isInstanceOf[SimpleLabeledDigraph[Int,Int]])
  }
}

class MultiLabeledAdjListSuite extends LabeledGraphSuite[Int, Int] with MultiGraphSuite[Int, Int,LabeledEdge[Int, Int]]{
  val graph = new MultiLabeledDirectedAdjacencyList[Int,Int]()


}




class MultiLabeledUndirectedAdjListSuite extends LabeledGraphSuite[Int, Int]
                                         with MultiGraphSuite[Int, Int, LabeledEdge[Int, Int]]
                                         with UndirectedGraphSuite[Int, Int, LabeledEdge[Int, Int]]
{
  val graph = new MultiLabeledUndirectedAdjacencyList[Int,Int]()


}

