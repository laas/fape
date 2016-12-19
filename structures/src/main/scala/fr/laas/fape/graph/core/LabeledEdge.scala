package fr.laas.fape.graph.core

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
class LabeledEdge[+V,+EdgeLabel](u:V, v:V, val l:EdgeLabel) extends Edge[V](u, v) {

  override def toString = "(%s, %s, %s)".format (u, v, l)
}
