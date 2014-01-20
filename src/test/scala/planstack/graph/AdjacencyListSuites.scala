package planstack.graph


import org.scalatest.Suite



class SimpleLabeledAdjListSuite extends LabeledGraphSuite[Int, Int] with SimpleGraphSuite[Int, Int, LabeledEdge[Int, Int]]{
  val graph = new SimpleLabeledDirectedAdjacencyList[Int,Int]()

  def testCompleteType {
    assert(graph.isInstanceOf[Predef.DirectedSimpleLabeledGraph[Int,Int]])
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