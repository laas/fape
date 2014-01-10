package planstack.graph

trait SimpleGraph[V, E <: Edge[V]] extends Graph[V,E] {

  override def addEdge(e:E) {
    deleteEdges(e.u, e.v)
    addEdgeImpl(e)
  }
}
