package planstack.graph

import org.scalatest.Suite


trait SimpleGraphSuite[V,E <: Edge[V]] extends BaseGraphSuite[V,E] {

  private def g = graph.asInstanceOf[SimpleGraph[Int,Edge[Int]]]

  def testSimpleType { assert(graph.isInstanceOf[SimpleGraph[Int,Edge[Int]]])}


  def testAddEdges {
    g.addVertex(1)
    g.addVertex(3)
    g.addEdge(new LabeledEdge[Int,Int](1, 3, 4))
    assert(g.edges.length === 1)
    g.addEdge(new LabeledEdge[Int,Int](1, 3, 4))
    assert(g.edges.length === 1)
  }

}
