package planstack.graph

trait UnlabeledGraph[V] extends Graph[V, Nothing, Edge[V]] {

  def addEdge(u:V, v:V) { addEdge(new Edge(u,v))}
}
