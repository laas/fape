package planstack.graph.printers

class NodeEdgePrinter[V, EL] {

  def printNode(node :V) : String = node.toString
  def printEdge(edge :EL) : String = edge.toString
}
