package planstack.graph.core


trait LabeledGraph[V, EL] extends Graph[V, EL, LabeledEdge[V, EL]] {

  def addEdge(u:V, v:V, l:EL) { addEdge(new LabeledEdge[V,EL](u, v, l)) }
}
