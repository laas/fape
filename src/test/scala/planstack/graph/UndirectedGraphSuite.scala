package planstack.graph

import planstack.graph.core.{UndirectedGraph, Edge, LabeledEdge}


trait UndirectedGraphSuite[V, EL, E <: Edge[V]] extends BaseGraphSuite[V, EL, E] {

  private def g = graph.asInstanceOf[UndirectedGraph[Int,Int, LabeledEdge[Int, Int]]]

  def testUndirectedType { assert(graph.isInstanceOf[UndirectedGraph[V,EL,E]])}

  // TODO : find to start from a clean graph
  def testAddUndirectedEdges {
    g.addVertex(8001)
    g.addVertex(8002)
    g.addVertex(8003)
    g.addEdge(new LabeledEdge[Int,Int](8001,8002, 20))
    assert(g.degree(8001) === 1)
    assert(g.degree(8002) === 1)
    assert(g.degree(8003) === 0)

    g.deleteEdges(8001, 8002)
    assert(g.degree(8001) === 0)
    assert(g.degree(8002) === 0)
    assert(g.degree(8003) === 0)
  }

}