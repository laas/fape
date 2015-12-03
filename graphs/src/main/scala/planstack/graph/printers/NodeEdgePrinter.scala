package planstack.graph.printers

import planstack.graph.core.Edge

trait NodeEdgePrinterInterface[V, EL, E <: Edge[V]] {
  def printNode(node :V) : String
  def printEdge(edge :EL) : String

  def excludeNode(node : V) : Boolean
  def excludeEdge(edge : E) : Boolean
}

class NodeEdgePrinter[V, EL, E <: Edge[V]] extends NodeEdgePrinterInterface[V,EL,E] {

  def printNode(node :V) : String = node.toString
  def printEdge(edge :EL) : String = edge.toString

  def excludeNode(node : V) : Boolean = false
  def excludeEdge(edge : E) : Boolean = excludeNode(edge.u) || excludeNode(edge.v)
}
