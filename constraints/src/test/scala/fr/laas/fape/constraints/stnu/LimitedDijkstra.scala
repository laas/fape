package fr.laas.fape.constraints.stnu

import fr.laas.fape.constraints.stnu.nilsson.EfficientIDC
import org.scalatest.FunSuite
import planstack.graph.GraphFactory

class LimitedDijkstra extends FunSuite {

  test("Limited dijsktra on simple graph") {
    val g = GraphFactory.getLabeledDigraph[Int, Int]

    val A = 0
    val B= 1
    val C = 2
    val D = 3
    val E = 4

    for(i <- 0 to 4) g.addVertex(i)

    g.addEdge(A, B, 3)
    g.addEdge(A, C, 5)
    g.addEdge(B, D, 4)
    g.addEdge(C, D, 1)
    g.addEdge(C, E, 20)

    val res = EfficientIDC.limitedDijkstra(A, g, 10)

    assert(res == (A,0)::(B,3)::(C,5)::(D,6)::Nil)


  }

}
