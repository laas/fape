package planstack.graph




trait DirectedGraph[V, E <: Edge[V]] extends Graph[V,E] {

  def edges(u:V, v:V) : Seq[E] = outEdges(u).filter(e => e.v == v)

  def inEdges(v:V) : Seq[E]

  def outEdges(u:V) : Seq[E]

  def inDegree(v:V) = inEdges(v).length

  def outDegree(v:V) = outEdges(v).length
}


object DirectedGraph {

  def apply[V]() = new MultiUnlabeledDirectedAdjacencyList[V]()
  def apply[V,EL]() = new MultiLabeledDirectedAdjacencyList[V, EL]()

}