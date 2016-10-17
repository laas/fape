package fr.laas.fape.constraints.stnu.nilsson

import planstack.graph.GraphFactory
import planstack.graph.algorithms.Algos
import planstack.graph.core.SimpleUnlabeledDigraph

class CCGraph(val g : SimpleUnlabeledDigraph[Int], var acyclic : Boolean) {

  def this() = this(GraphFactory.getSimpleUnlabeledDigraph[Int], true)
  def this(cc : CCGraph) = this(cc.g.cc, cc.acyclic)

  def addEdge(from:Int, to:Int) {
    if(!g.contains(from))
      g.addVertex(from)
    if(!g.contains(to))
      g.addVertex(to)
    if(g.edge(from, to).isEmpty) {
      g.addEdge(from, to)
      update(from, to)
    }
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

  def cc() = new CCGraph(this)

}
