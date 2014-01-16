package planstack.graph

trait UndirectedGraph[V, E <: Edge[V]] extends Graph[V,E] {

  /**
   * Returns all edges touching v
   * @param v
   * @return
   */
  def edges(v:V) : Seq[E]

  /**
   * Return all edges (u,v) and (v,u)
   * @param u
   * @param v
   * @return
   */
  def edges(u:V, v:V): Seq[E] = edges(u).filter(e => e.u == v || e.v == v)
  //TODO: This is wrong if we are looking for edges (u,u)

  def degree(v:V) = edges(v).length

  /**
   * return all vertices u such as their is an edge (u,v) or (v,u)
   * @param v
   * @return
   */
  def neighbours(v:V) = edges(v).map(e => if(e.u != v) e.u else e.v).toSet
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