package planstack.graph.printers

import planstack.graph.core.Edge

class NodeEdgePrinter[V, EL, E <: Edge[V]] {

  def printNode(node :V) : String = node.toString
  def printEdge(edge :EL) : String = edge.toString

  def excludeNode(node : V) : Boolean = false
  def excludeEdge(edge : E) : Boolean = excludeNode(edge.u) || excludeNode(edge.v)
}
