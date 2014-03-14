package planstack.graph.core

import scala.collection.JavaConversions._

trait Graph[V, +EL, E <: Edge[V]] {

  def addVertex(v:V) : Int

  def vertices : Seq[V]

  /** Returns all nodes/vertices in the graph */
  def jVertices = seqAsJavaList(vertices)

  def addEdge(e:E)

  protected def addEdgeImpl(e:E)

  def contains(v:V) : Boolean

  def edges() : Seq[E]

  def edges(u:V, v:V) : Seq[E]

  def numVertices : Int

  /**
   * Removes all edges from u to v in the graph
   * @param u
   * @param v
   */
  def deleteEdges(u:V, v:V)

  def cc : Graph[V, EL, E]
}




object Graph {

  def apply[V]() = new impl.MultiUnlabeledDirectedAdjacencyList[V]()
  def apply[V, EdgeLabel]() = new impl.MultiLabeledDirectedAdjacencyList[V, EdgeLabel]()

}
