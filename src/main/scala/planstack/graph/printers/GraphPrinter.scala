package planstack.graph.printers

import planstack.graph._
import scala.collection.mutable

//TODO: support tree structure

/** SOme initial implementation of a graph printer.
  * This one generates a dot syntax for the graph.
  *
  * @param g
  * @tparam N
  * @tparam E
  *
class OldGraphPrinter[N, E](val g:GenGraph[N,E]) {

  def writeToFile(filename: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new java.io.File(filename))
    try pw.write(s) finally pw.close()
  }

  val header = "digraph g {\n  node [shape=plaintext] rankdir=\"RL\"\n;"
  val footer = "\n}"

  def toDotString(str:String) = "\"" + str + "\""

  /**
   * Gives a String representation of a vertex.
   * If the graph has InGraphVertices, it uses those for output
   * @param id
   * @return
   */
  def nodeId2Str(id:Int) : String = {
    g match {
      case g1:InGraphVerticesGenGraph[N,E] => toDotString(""+ id +":"+ g1.vertex(id))
      case g => toDotString(id.toString)
    }
  }

  /**
   * Creates the dot representation of the graph.
   * @return
   */
  def graph2DotString : String = {
    var out = header
    g.getEdges.foreach(e =>{
      out += "  %s -> %s\n".format(nodeId2Str(e.orig), nodeId2Str(e.dest))
    })
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
*/

trait GraphPrinter {

}

class GraphDotPrinter[V,E <: Edge[V]](val g:Graph[V,E]) extends GraphPrinter {

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
      case udg:UndirectedGraph[V,E] => {
        if(e.isLabeled) {
          "--" //todo use edge label (i.e. fix typing problem of LabeledGraph)
        } else {
          "--"
        }
      }
      case dg:DirectedGraph[V,E] => "->"
    }

    "  " + node2Str(e.u) +" "+ link +" "+ node2Str(e.v)
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
