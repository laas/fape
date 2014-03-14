package planstack.graph.algorithms

import planstack.graph.core.{LabeledEdge, Graph, DirectedGraph, Edge}

class ReverseDirectedGraph[V, EL](val dg : DirectedGraph[V, Nothing, Edge[V]]) extends DirectedGraph[V, Nothing, Edge[V]] {
  type E = Edge[V]

  private def edgeInverser(e : E) : E = {
    e match {
      case le : LabeledEdge[V, EL] => new LabeledEdge[V, EL](le.v, le.u, le.l)
      case e : Edge[V] => new Edge[V](e.v, e.u)
    }
  }

  def inEdges(v: V): Seq[E] = dg.outEdges(v).map(edgeInverser(_))

  def outEdges(u: V): Seq[E] = dg.inEdges(u).map(edgeInverser(_))

  def addVertex(v: V): Int = ???

  def vertices(): Seq[V] = dg.vertices

  def addEdge(e: E): Unit = ???

  protected def addEdgeImpl(e: E): Unit = ???

  def contains(v: V): Boolean = dg.contains(v)

  def edges(): Seq[E] = dg.edges().map(edgeInverser(_))

  def numVertices: Int = dg.numVertices

  /**
   * Removes all edges from u to v in the graph
   * @param u
   * @param v
   */
  def deleteEdges(u: V, v: V): Unit = ???

  def cc(): Graph[V, Nothing, E] = ???
}
