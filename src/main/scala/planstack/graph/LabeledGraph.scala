package planstack.graph

trait LabeledGraph[V, EdgeLabel] extends Graph[V, LabeledEdge[V, EdgeLabel]] {

  def addEdge(u:V, v:V, l:EdgeLabel) { addEdge(new LabeledEdge[V,EdgeLabel](u, v, l)) }
}
