package planstack.graph

trait MultiGraph[V, E <:Edge[V]] extends Graph[V,E] {

  override def addEdge(e:E) { addEdgeImpl(e) }
}
