package fr.laas.fape.graph.core

import fr.laas.fape.graph.core

/** Specialisation og [[core.Graph]] where there might be more than one edge between two vertices.
  *
  * @tparam V Type of vertices
  * @tparam EL Type of the edges' labels. Should be set to Nothing if the graph has no labels on its edges.
  * @tparam E Type of the edges. Should be either Edge[V] or LabeledEdge[V, EL]
  */
trait MultiGraph[V, EL, E <:Edge[V]] extends Graph[V,EL,E] {

  override def addEdge(e:E) { addEdgeImpl(e) }
}
