package fr.laas.fape.graph

import fr.laas.fape.graph.core.{Edge, LabeledEdge, SimpleGraph}


trait SimpleGraphSuite[V,EL,E <: Edge[V]] extends BaseGraphSuite[V,EL,E] {

  private def g = graph.asInstanceOf[SimpleGraph[Int,EL,Edge[Int]]]

  test("Add Edges") {
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
