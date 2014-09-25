package planstack.graph

import org.scalatest.FunSuite
import planstack.graph.core.{Edge, Graph, MultiGraph, UnlabeledGraph}


class GraphSuite extends FunSuite {

  val g = Graph[Int]()
  g.addVertex(1)
  g.addVertex(4)
  g.addEdge(1, 4)

  assert(g.edges.length === 1)

  test("Graph Types") {
    assert(g.isInstanceOf[Graph[Int, Nothing, Edge[Int]]])
    assert(g.isInstanceOf[MultiGraph[Int, Nothing, Edge[Int]]])
    assert(g.isInstanceOf[UnlabeledGraph[Int]])
  }

}
