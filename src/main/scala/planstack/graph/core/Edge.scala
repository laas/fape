package planstack.graph.core

/** Representation of an edge between two vertices.
  *
  * Notable subclass is [[planstack.graph.core.LabeledEdge]] that adds a third attribute: a label on the edge.
  *
  * @param u Source
  * @param v Target
  * @tparam V Type of the Vertices
  */
class Edge[+V](val u:V, val v:V) {

  override def toString = "(%s, %s)".format(u, v)
}
