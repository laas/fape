package planstack.graph


import org.scalatest.Suite



class SimpleLabeledAdjListSuite extends LabeledGraphSuite[Int, Int] with SimpleGraphSuite[Int, LabeledEdge[Int, Int]]{
  val graph = new SimpleLabeledAdjacencyList[Int,Int]()
}

class MultiLabeledAdjListSuite extends LabeledGraphSuite[Int, Int] with MultiGraphSuite[Int, LabeledEdge[Int, Int]]{
  val graph = new MultiLabeledAdjacencyList[Int,Int]()
}