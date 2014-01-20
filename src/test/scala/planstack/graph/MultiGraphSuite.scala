package planstack.graph

import org.scalatest.Suite
import planstack.graph.core.{MultiGraph, Edge, LabeledEdge}


trait MultiGraphSuite[V, EL, E <: Edge[V]] extends BaseGraphSuite[V, EL, E]{

  private def g = graph.asInstanceOf[MultiGraph[Int, EL, Edge[Int]]]

  def testMultiType { assert(graph.isInstanceOf[MultiGraph[Int, EL, Edge[Int]]])}

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
