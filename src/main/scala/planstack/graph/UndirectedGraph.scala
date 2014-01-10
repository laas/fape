package planstack.graph

trait UndirectedGraph[V, E <: Edge[V]] extends Graph[V,E] {

  /**
   * Returns all edges touching v
   * @param v
   * @return
   */
  def edges(v:V) : Seq[E]

  def degree(v:V) = edges(v).length
}
