package planstack.graph

import org.scalatest.Suite

import planstack.graph._
import planstack.graph.core.{UnlabeledGraph, MultiGraph, Graph, Edge}


class GraphSuite extends Suite {

  val g = Graph[Int]()
  g.addVertex(1)
  g.addVertex(4)
  g.addEdge(1, 4)

  assert(g.edges.length === 1)

  def testGraphTypes() {
    assert(g.isInstanceOf[Graph[Int, Nothing, Edge[Int]]])
    assert(g.isInstanceOf[MultiGraph[Int, Nothing, Edge[Int]]])
    assert(g.isInstanceOf[UnlabeledGraph[Int]])
  }

}
