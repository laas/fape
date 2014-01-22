package planstack.graph

import planstack.graph.core.{SimpleGraph, Edge, LabeledEdge}


trait SimpleGraphSuite[V,EL,E <: Edge[V]] extends BaseGraphSuite[V,EL,E] {

  private def g = graph.asInstanceOf[SimpleGraph[Int,EL,Edge[Int]]]

  def testSimpleType { assert(graph.isInstanceOf[SimpleGraph[Int,EL,Edge[Int]]])}


  def testAddEdges {
    val u = newVert()
    val v = newVert()
    g.addVertex(u)
    g.addVertex(v)
    g.addEdge(new LabeledEdge[Int,Int](u, v, 4))
    assert(g.edges.length === 1)
    g.addEdge(new LabeledEdge[Int,Int](u, v, 4))
    assert(g.edges.length === 1)
  }

}
