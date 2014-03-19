package planstack.graph.core

import planstack.graph.Predef._
import scala.Some

/** Representation of a simple graph (graph having at most one edge between two vertices).
  *
  * Adding an edge (u, v) to this graph replaces any previous (u, v) edge.
  *
  * @tparam V Type of vertices
  * @tparam EL Type of the edges' labels. Should be set to Nothing if the graph has no labels on its edges.
  * @tparam E Type of the edges. Should be either Edge[V] or LabeledEdge[V, EL]
  */
trait SimpleGraph[V, EL, E <: Edge[V]] extends Graph[V,EL,E] {

  /**
   * Look for an instance of an edge e=(u,v) in the graph.
   *
   * Returns Some(e) if this edge is present. None otherwise.
   *
   * @param u
   * @param v
   * @return
   */
  def edge(u:V, v:V) : Option[E] = edges(u:V, v:V) match {
    case Nil => None
    case List(e) => Some(e)
    case _ => Ex( "Simple graph with multiple instance of edge ("+u+", "+v+")" )
  }

  override def addEdge(e:E) {
    deleteEdges(e.u, e.v)
    addEdgeImpl(e)
  }
}
