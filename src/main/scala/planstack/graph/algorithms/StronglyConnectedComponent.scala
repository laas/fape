package planstack.graph.algorithms

import planstack.graph.core.{SimpleUnlabeledDigraph, UnlabeledDigraph, Edge, DirectedGraph}
import scala.collection.mutable
import planstack.graph.core.impl.SimpleUnlabeledDirectedAdjacencyList
import planstack.graph.printers.GraphDotPrinter
import scala.collection.JavaConversions._

class StronglyConnectedComponent[V](dag : DirectedGraph[V, Nothing, Edge[V]]) {

  val reverseDAG = new ReverseDirectedGraph(dag)

  val marked = new mutable.HashSet[V]
  val groupIds = new mutable.HashMap[V, Int]()
  val groups = new mutable.HashMap[Int, mutable.HashSet[V]]
  var count = 0


  for(v <- Algos.depthFirstOrderReversePost(reverseDAG)) {
    if(!marked.contains(v)) {
      groups += ((count, new mutable.HashSet[V]()))
      dfs(v)
      count += 1
    }
  }

  private def dfs(v : V) {
    marked += v
    groupIds += ((v, count))
    groups(count) += v
    for(child <- dag.children(v)) {
      if(!marked.contains(child)) {
        dfs(child)
      }
    }

  }

  def reducedGraph() : SimpleUnlabeledDigraph[mutable.Set[V]] = {
    val reduced = new SimpleUnlabeledDirectedAdjacencyList[mutable.Set[V]]()

    groups.values.foreach(reduced.addVertex(_))

    dag.edges().foreach(e => {
      val g1 = groupIds(e.u)
      val g2 = groupIds(e.v)
      if(g1 != g2)
        reduced.addEdge(groups(g1), groups(g2))
    })

    val printer = new GraphDotPrinter(reduced)
    printer.print2Dot("/home/abitmonn/tmp/g.dot")

    reduced
  }

  def topologicalSortOfReducedGraph = {
    val reduced = reducedGraph()
    Algos.topologicalSorting(reduced)
  }

  def jTopologicalSortOfReducedGraph = {
    seqAsJavaList(topologicalSortOfReducedGraph.map(setAsJavaSet(_)))
  }



}

object Test extends App {
  val g = DirectedGraph[Int]()

  for(i <- 0 to 5)
    g.addVertex(i)
  g.addEdge(0, 1)
  g.addEdge(0, 2)
  g.addEdge(1, 3)
  g.addEdge(0, 4)
  g.addEdge(1, 5)
  g.addEdge(5, 3)
  g.addEdge(5, 0)
  g.addEdge(3, 2)
  g.addEdge(2, 3)


  val scc = new StronglyConnectedComponent(g)

  println(scc.groups.values.mkString("\n"))
}

