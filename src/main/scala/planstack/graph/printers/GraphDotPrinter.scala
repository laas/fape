package planstack.graph.printers

import scala.collection.mutable
import planstack.graph.core._

/** Simple translator from [[planstack.graph.core.Graph]] to the DOT syntax of graphviz
  *
  * @param g Graph to export.
  * @tparam V
  * @tparam EL
  * @tparam E
  */
class GraphDotPrinter[V,EL,E <: Edge[V]](val g: Graph[V,EL,E], val nep :NodeEdgePrinter[V,EL]) {

  def this(g :Graph[V,EL,E]) = this(g, new NodeEdgePrinter[V,EL])

  def writeToFile(filename: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new java.io.File(filename))
    try pw.write(s) finally pw.close()
  }

  val header = g match {
    case dg:DirectedGraph[V,EL,E] => "digraph g {\n  node [shape=plaintext] rankdir=\"TB\"\n;"
    case _ => "digraph g {\n  node [shape=plaintext] rankdir=\"TB\"\n;"  //TODO not a digraph
  }
  val footer = "\n}"

  def edge2Str(e:E) = {
    val link = g match {
      case udg:UndirectedGraph[V,EL,E] => "--"
      case dg:DirectedGraph[V,EL,E] => "->"
    }
    val label = e match {
      case e:LabeledEdge[V,EL] => "[label=\"%s\"]".format(nep.printEdge(e.l))
      case _ => ""
    }
    "  " + node2Str(e.u) +" "+ link +" "+ node2Str(e.v) + label
  }

  val nodeId = mutable.Map[V, Int]()

  def node2Str(v:V) = {
    if(!nodeId.contains(v))
      nodeId(v) = nodeId.size
    "\"" + nodeId(v) +":"+ nep.printNode(v) + "\""
  }

  /**
   * Creates the dot representation of the graph.
   * @return
   */
  def graph2DotString : String = {
    var out = header
    out += g.edges().map(edge2Str(_)).mkString("\n")
    out += footer
    out
  }

  /**
   * Print the graph to a file in dot format.
   * @param file File to write the graph to
   */
  def print2Dot(file:String) {
    writeToFile(file, graph2DotString)
  }

}
