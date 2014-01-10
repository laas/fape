package planstack.graph

trait UnlabeledGraph[V] extends Graph[V, Edge[V]] {

  def addEdge(u:V, v:V) { addEdge(new Edge(u,v))}
}
