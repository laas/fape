package planstack.constraints.stnu

import planstack.graph.GraphFactory
import planstack.graph.algorithms.Algos

class CCGraph {

  val g = GraphFactory.getSimpleUnlabeledDigraph[Int]

  var acyclic = true

  def addEdge(from:Int, to:Int) {
    if(!g.contains(from))
      g.addVertex(from)
    if(!g.contains(to))
      g.addVertex(to)
    if(g.edge(from, to).isEmpty) {
      g.addEdge(from, to)
      update(from, to)
    }
    acyclic
  }

  def update(from:Int, to:Int) {
    tarjan()
  }

  def tarjan() {
    try {
      Algos.topologicalSorting(g)
      acyclic = true
    } catch {
      case e:Exception => acyclic = false
    }
  }

}
