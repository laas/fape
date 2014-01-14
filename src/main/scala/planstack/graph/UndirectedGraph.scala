package planstack.graph

trait UndirectedGraph[V, E <: Edge[V]] extends Graph[V,E] {

  /**
   * Returns all edges touching v
   * @param v
   * @return
   */
  def edges(v:V) : Seq[E]

  def edges(u:V, v:V): Seq[E] = edges(u).filter(e => e.u == v || e.v == v)

  def degree(v:V) = edges(v).length
}


object UndirectedGraph {

  /**
   * Get the default labeled Undirected graph. This graph is a multi graph.
   * @tparam V Type of the vertices
   * @tparam EdgeLabel Type of the label on the edges
   * @return
   */
  def apply[V, EdgeLabel]() = new MultiLabeledUndirectedAdjacencyList[V, EdgeLabel]()
}