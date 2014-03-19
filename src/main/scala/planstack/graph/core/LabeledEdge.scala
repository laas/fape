package planstack.graph.core


/** Representation of a labeled edge between two vertices.
  *
  * The label type is defined as a type parameter.
  *
  * @param u Source
  * @param v Target
  * @param l Label
  * @tparam V Type of the Vertices
  * @tparam EdgeLabel
  */
final class LabeledEdge[V,EdgeLabel](u:V, v:V, val l:EdgeLabel) extends Edge[V](u, v) {

  /** Label of the edge */
  @deprecated
  def e = l

  override def toString = "(%s, %s, %s)".format (u, v, l)
}
