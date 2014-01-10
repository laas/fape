package planstack.graph

class LabeledEdge[V,EdgeLabel](u:V, v:V, val e:EdgeLabel) extends Edge[V](u, v)
