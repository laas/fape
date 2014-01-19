package planstack.graph.printers

import planstack.graph._
import scala.collection.mutable




class GraphDotPrinter[V,E <: Edge[V]](val g:Graph[V,E]) {

  def writeToFile(filename: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new java.io.File(filename))
    try pw.write(s) finally pw.close()
  }

  val header = g match {
    case dg:DirectedGraph[V,E] => "digraph g {\n  node [shape=plaintext] rankdir=\"TB\"\n;"
    case _ => "digraph g {\n  node [shape=plaintext] rankdir=\"TB\"\n;"  //TODO not a digraph
  }
  val footer = "\n}"

  def edge2Str(e:E) = {
    val link = g match {
      case udg:UndirectedGraph[V,E] => "--"
      case dg:DirectedGraph[V,E] => "->"
    }
    val label =
      if(e.isLabeled)
        "[label=\"" + e.edgeLabel2String + "\"]"
      else
        ""

    "  " + node2Str(e.u) +" "+ link +" "+ node2Str(e.v) + label
  }

  val nodeId = mutable.Map[V, Int]()

  def node2Str(v:V) = {
    if(!nodeId.contains(v))
      nodeId(v) = nodeId.size
    "\"" + nodeId(v) +":"+ v.toString + "\""
  }

  /**
   * Creates the dot representation of the graph.
   * @return
   */
  def graph2DotString : String = {
    var out = header
    g.edges.foreach(e => out += edge2Str(e))
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
