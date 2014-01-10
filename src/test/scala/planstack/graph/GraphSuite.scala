package planstack.graph

import org.scalatest.Suite

import planstack.graph._


class GraphSuite extends Suite {

  val g = Graph[Int]()
  g.addVertex(1)
  g.addVertex(4)
  g.addEdge(1, 4)

  assert(g.edges.length === 1)

  def testGraphTypes() {
    assert(g.isInstanceOf[Graph[Int, Edge[Int]]])
    assert(g.isInstanceOf[MultiGraph[Int,Edge[Int]]])
    assert(g.isInstanceOf[UnlabeledGraph[Int]])
  }

}
