package planstack.graph

import org.scalatest.Suite


trait LabeledGraphSuite[V,EdgeLabel] extends BaseGraphSuite[V, LabeledEdge[V,EdgeLabel]] {

  private def g = graph.asInstanceOf[LabeledGraphSuite[V,EdgeLabel]]

  def testLabeledType { assert(graph.isInstanceOf[LabeledGraph[V,EdgeLabel]])}

}
