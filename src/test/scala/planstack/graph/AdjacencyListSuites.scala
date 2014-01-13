package planstack.graph


import org.scalatest.Suite



class SimpleLabeledAdjListSuite extends LabeledGraphSuite[Int, Int] with SimpleGraphSuite[Int, LabeledEdge[Int, Int]]{
  val graph = new SimpleLabeledDirectedAdjacencyList[Int,Int]()
}

class MultiLabeledAdjListSuite extends LabeledGraphSuite[Int, Int] with MultiGraphSuite[Int, LabeledEdge[Int, Int]]{
  val graph = new MultiLabeledDirectedAdjacencyList[Int,Int]()
}




class MultiLabeledUndirectedAdjListSuite extends LabeledGraphSuite[Int, Int]
                                         with MultiGraphSuite[Int, LabeledEdge[Int, Int]]
                                         with UndirectedGraphSuite[Int, LabeledEdge[Int, Int]]
{
  val graph = new MultiLabeledUndirectedAdjacencyList[Int,Int]()
}