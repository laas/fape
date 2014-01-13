package planstack.graph

class LabeledEdge[V,EdgeLabel](u:V, v:V, val e:EdgeLabel) extends Edge[V](u, v) {
  type LabelType = EdgeLabel

  override def toString = "(%s, %s, %s)".format (u, v, e)

  override val isLabeled = true
}
