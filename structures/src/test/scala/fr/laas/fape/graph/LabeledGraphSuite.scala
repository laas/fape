package fr.laas.fape.graph

import fr.laas.fape.graph.core.{LabeledEdge, LabeledGraph}


trait LabeledGraphSuite[V,EdgeLabel] extends BaseGraphSuite[V, EdgeLabel, LabeledEdge[V,EdgeLabel]] {

  private def g = graph.asInstanceOf[LabeledGraph[V,EdgeLabel]]

  test("LabeledType") { assert(graph.isInstanceOf[LabeledGraph[V,EdgeLabel]])}

}
