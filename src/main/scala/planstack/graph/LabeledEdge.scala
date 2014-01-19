package planstack.graph

class LabeledEdge[V,EdgeLabel](u:V, v:V, val l:EdgeLabel) extends Edge[V](u, v) {
  type LabelType = EdgeLabel

  @deprecated
  def e = l

  override def toString = "(%s, %s, %s)".format (u, v, l)

  override val isLabeled = true

  override def edgeLabel2String = l.toString
}
