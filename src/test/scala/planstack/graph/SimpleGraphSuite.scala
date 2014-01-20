package planstack.graph

import planstack.graph.core.{SimpleGraph, Edge, LabeledEdge}


trait SimpleGraphSuite[V,EL,E <: Edge[V]] extends BaseGraphSuite[V,EL,E] {

  private def g = graph.asInstanceOf[SimpleGraph[Int,EL,Edge[Int]]]

  def testSimpleType { assert(graph.isInstanceOf[SimpleGraph[Int,EL,Edge[Int]]])}


  def testAddEdges {
    g.addVertex(1)
    g.addVertex(3)
    g.addEdge(new LabeledEdge[Int,Int](1, 3, 4))
    assert(g.edges.length === 1)
    g.addEdge(new LabeledEdge[Int,Int](1, 3, 4))
    assert(g.edges.length === 1)
  }

}
