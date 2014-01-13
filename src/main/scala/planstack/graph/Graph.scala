package planstack.graph

trait Graph[V, E <: Edge[V]] {

  def addVertex(v:V) : Int

  def addEdge(e:E)

  protected def addEdgeImpl(e:E)

  def contains(v:V) : Boolean

  def edges() : Seq[E]

  def numVertices : Int

  /**
   * Removes all edges from u to v in the graph
   * @param u
   * @param v
   */
  def deleteEdges(u:V, v:V)
}




object Graph {

  def apply[V]() = new MultiUnlabeledDirectedAdjacencyList[V]()
  def apply[V, EdgeLabel]() = new MultiLabeledDirectedAdjacencyList[V, EdgeLabel]()

}
