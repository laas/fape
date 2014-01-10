package planstack.graph

import org.scalatest.Suite


trait MultiGraphSuite[V, E <: Edge[V]] extends BaseGraphSuite[V, E]{

  private def g = graph.asInstanceOf[MultiGraph[Int, Edge[Int]]]

  def testMultiType { assert(graph.isInstanceOf[MultiGraph[Int, Edge[Int]]])}

  def testAddEdges {
    g.addVertex(1)
    g.addVertex(2)
    g.addEdge(new LabeledEdge[Int,Int](1, 2, 4))
    assert(g.edges.length === 1)
    g.addEdge(new LabeledEdge[Int,Int](1, 2, 5))
    assert(g.edges.length === 2)
    g.addEdge(new LabeledEdge[Int,Int](1, 2, 5))
    assert(g.edges.length === 3, "Edges with exactly same values")
  }

}
