package fr.laas.fape.graph

import fr.laas.fape.graph.core.{Edge, LabeledEdge, UndirectedGraph}


trait UndirectedGraphSuite[V, EL, E <: Edge[V]] extends BaseGraphSuite[V, EL, E] {

  private def g = graph.asInstanceOf[UndirectedGraph[Int,Int, LabeledEdge[Int, Int]]]

  test("UndirectedType") { assert(graph.isInstanceOf[UndirectedGraph[V,EL,E]])}

  // TODO : find to start from a clean graph
  test("AddUndirectedEdges") {
    val u = newVert()
    g.addVertex(u)
    val v = newVert()
    g.addVertex(v)
    val w = newVert()
    g.addVertex(w)
    
    g.addEdge(new LabeledEdge[Int,Int](u,v, 20))
    assert(g.degree(u) === 1)
    assert(g.degree(v) === 1)
    assert(g.degree(w) === 0)

    g.deleteEdges(u, v)
    assert(g.degree(u) === 0)
    assert(g.degree(v) === 0)
    assert(g.degree(w) === 0)
  }

}