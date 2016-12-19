package fr.laas.fape.graph

import fr.laas.fape.graph.core.SimpleLabeledDigraph
import fr.laas.fape.graph.core.impl.SimpleLabeledDirectedAdjacencyList


object Predef {

  def Ex(m:String) = throw new Exception(m)

  def newSimpleLabeledDigraph[V, EL] : SimpleLabeledDigraph[V,EL] = new SimpleLabeledDirectedAdjacencyList[V, EL]()


}