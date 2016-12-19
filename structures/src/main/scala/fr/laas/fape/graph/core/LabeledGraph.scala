package fr.laas.fape.graph.core

import fr.laas.fape.graph.core

/** Specialisation of [[core.Graph]] where edges have a label attached.
  *
  * Hence the type of the edges is [[LabeledEdge]]
  *
  * @tparam V Type of vertices
  * @tparam EL Type of the edges' labels.
  */
trait LabeledGraph[V, EL] extends Graph[V, EL, LabeledEdge[V, EL]] {

  /** Adds a new edge to the graph.
    * Source and target of the edge should be already present in the graph.
    *
    * @param u Source of the edge.
    * @param v Target of the edge.
    * @param l Label of the edge.
    */
  def addEdge(u:V, v:V, l:EL) { addEdge(new LabeledEdge[V,EL](u, v, l)) }
}
