package fr.laas.fape.constraints.stn

import planstack.graph.core.LabeledEdge

class LabeledEdgeWithID[+V,+EL,ID](u:V, v:V, l:EL, val id:ID)
  extends LabeledEdge[V,EL](u, v, l)

