package planstack.graph

import planstack.graph.core._
import planstack.graph.core.impl.SimpleLabeledDirectedAdjacencyList


object Predef {

  def Ex(m:String) = throw new Exception(m)

  def newSimpleLabeledDigraph[V, EL] : SimpleLabeledDigraph[V,EL] = new SimpleLabeledDirectedAdjacencyList[V, EL]()


}