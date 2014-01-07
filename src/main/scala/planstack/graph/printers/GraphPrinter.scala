package planstack.graph.printers

import planstack.graph.{InGraphVerticesGenGraph, GenGraph}

//TODO: support tree structure

/** SOme initial implementation of a graph printer.
  * This one generates a dot syntax for the graph.
  *
  * @param g
  * @tparam N
  * @tparam E
  */
class GraphPrinter[N, E](val g:GenGraph[N,E]) {

  def writeToFile(filename: String, s: String): Unit = {
    val pw = new java.io.PrintWriter(new java.io.File(filename))
    try pw.write(s) finally pw.close()
  }

  val header = "digraph g {\n  node [shape=plaintext]\n;"
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
