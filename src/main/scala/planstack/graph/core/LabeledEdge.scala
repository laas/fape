package planstack.graph.core



final class LabeledEdge[V,EdgeLabel](u:V, v:V, val l:EdgeLabel) extends Edge[V](u, v) {
  @deprecated
  def e = l

  override def toString = "(%s, %s, %s)".format (u, v, l)
}
