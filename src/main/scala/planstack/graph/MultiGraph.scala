package planstack.graph

trait MultiGraph[V, EL, E <:Edge[V]] extends Graph[V,EL,E] {

  override def addEdge(e:E) { addEdgeImpl(e) }
}
