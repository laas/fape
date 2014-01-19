package planstack.graph

import Predef._

trait SimpleGraph[V, E <: Edge[V]] extends Graph[V,E] {

  /**
   * Look for an instance of an edge e=(u,v) in the graph.
   *
   * Returns Some(e) if this edge is present. None otherwise.
   *
   * @param u
   * @param v
   * @return
   */
  def edge(u:V, v:V) : Option[E] = {
    val _edges = edges(u:V, v:V)
    _edges.length match {
      case 0 => None
      case 1 => Some(_edges.head)
      case _ => Ex( "Simple graph with multiple instance of edge ("+u+", "+v+")" )
    }
  }

  override def addEdge(e:E) {
    deleteEdges(e.u, e.v)
    addEdgeImpl(e)
  }
}
