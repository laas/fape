package planstack.graph.printers

import planstack.graph.core._

import scala.collection.mutable

/** Simple translator from [[planstack.graph.core.Graph]] to the DOT syntax of graphviz.
  *
  * The resulting file will contain something like:
  * {{{
  *   digraph g {
  *     node [shape=plaintext] rankdir="TB";
  *     0 [label="String representation of node 0"];
  *     1 [label="String representation of node 1"];
  *     2 [label="String representation of node 2"];
  *     ...
  *     0 -> 1 [label="String representation of the edge label"];
  *     0 -> 2 [label="String representation of the edge label"];
  *     ...
  *  }
  * }}}
  *
  * Such a graph, written to graph.dot can be converted using the dot command line utility, for instance:
  * `dot -Tps graph.dot > graph.ps`
  *
  * @param g Graph to export.
  * @param nep A node/edge printer that is used to translate both nodes and edges' labels to string.
  * @tparam V Type of vertices.
  * @tparam EL Type of edge labels.
  * @tparam E Type of edges.
  */
class GraphDotPrinter[V,EL,E <: Edge[V]](val g: Graph[V,EL,E],
                                         val printNode : (V => String),
                                         val printEdge : (EL => String),
                                         val excludeNode : (V => Boolean),
                                         val excludeEdge : (E => Boolean)) {

  def this (g :Graph[V,EL,E], nep :NodeEdgePrinter[V, EL, E]) =
    this(g, nep.printNode, nep.printEdge, nep.excludeNode, nep.excludeEdge)

  def this(g :Graph[V,EL,E]) = this(g, new NodeEdgePrinter[V,EL,E])

  def writeToFile(filename: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new java.io.File(filename))
    try pw.write(s) finally pw.close()
  }

  val header = g match {
    case dg:DirectedGraph[V,EL,E] => "digraph g {\n  node [shape=plaintext] rankdir=\"TB\";\n\n"
    case _ => "digraph g {\n  node [shape=plaintext] rankdir=\"TB\";\n\n"  //TODO not a digraph
  }
  val footer = "\n}"

  /** Translate an edge to dot syntax such as `0 -- 1;` (for undirected graphs) or `0 -> 1;`
    * where 0 and 1 are the ids of respectively the source and target node of the graph.
    * @param e Edge to output.
    * @return Dot syntax for the declaration of the edge.
    */
  def edge2Str(e:E) = {
    val link = g match {
      case udg:UndirectedGraph[V,EL,E] => "--"
      case dg:DirectedGraph[V,EL,E] => "->"
    }
    val label = e match {
      case e:LabeledEdge[V,EL] => " [label=\"%s\"]".format(printEdge(e.l).replaceAll("\n", "\\\\n"))
      case _ => ""
    }
    "  " + nodeId(e.u) +" "+ link +" "+ nodeId(e.v) + label +";"
  }

  private val nodeId = mutable.Map[V, Int]()

  /** Translate a node to dot syntax, for instance `1 [label="some text"];`
    * where 1 is the id of the node that will be used to draw edges. and `some text`
    * is the string representation of the node given by a [[planstack.graph.printers.NodeEdgePrinter]].
    *
    * @param v Vertex to output.
    * @return Declaration of the vertex in dot syntax.
    */
  def node2Str(v:V) = {
    if(!nodeId.contains(v)) {
      nodeId(v) = nodeId.size
      "  " + nodeId(v) +" [label=\""+printNode(v).replaceAll("\n", "\\\\n")+"\"];\n"
    } else {
      ""
    }
  }

  /**
   * Creates the dot representation of the graph.
   * @return
   */
  def graph2DotString : String = {
    var out = header
    for(v <- g.vertices.filter(!excludeNode(_)))
      out += node2Str(v)
    out += g.vertices.filter(!excludeNode(_)).map(node2Str(_)).mkString("\n")
    out += "\n"
    out += g.edges().filter(!excludeEdge(_)).map(edge2Str(_)).mkString("\n")
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
