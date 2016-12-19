package fr.laas.fape.graph

import fr.laas.fape.graph.core.impl.MultiLabeledUndirectedAdjacencyList
import fr.laas.fape.graph.core.impl.intindexed.DirectedMultiLabeledIIAdjList
import fr.laas.fape.graph.core.{LabeledEdge, LabeledGraph}
import org.scalatest.FunSuite

class EdgeRemovalSuite extends FunSuite {

  test("Removal by ref on labeled graph") {
    val g : LabeledGraph[Int,Int] = GraphFactory.getMultiLabeledDigraph[Int,Int]

    for(g:LabeledGraph[Int,Int]
        <- List[LabeledGraph[Int,Int]](GraphFactory.getMultiLabeledDigraph[Int,Int],
                new DirectedMultiLabeledIIAdjList[Int](),
                new MultiLabeledUndirectedAdjacencyList[Int,Int]())) {

      g.addVertex(0)
      g.addVertex(1)
      g.addVertex(2)

      g.addEdge(0, 1, 0)
      val toRemove = new LabeledEdge[Int, Int](0, 1, 0)
      g.addEdge(toRemove)
      g.addEdge(new LabeledEdge[Int, Int](0, 1, 0))
      g.addEdge(0, 1, 3)
      g.addEdge(0, 2, 0)
      g.addEdge(1, 2, 0)

      val previousSize = g.edges().size
      g.deleteEdge(toRemove)
      assert(g.edges().size == previousSize - 1)
    }
  }

}
