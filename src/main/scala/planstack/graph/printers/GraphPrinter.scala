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

  val header = "digraph g {\n  node [shape=plaintext];"
  val footer = "\n}"

  def rmSpaces(str:String) =
    str.replaceAll("-","")
      .replaceAll(",","")
      .replaceAll("\\(","")
      .replaceAll("\\)","")
      .replaceAll(">","")
      .replaceAll(" ","")
      .replaceAll("=","")
      .replaceAll("\\.","")

  def nodeId2Str(id:Int) : String = {
    g match {
      case g1:InGraphVerticesGenGraph[N,E] => rmSpaces(g1.vertex(id).toString)
      case g => rmSpaces(id.toString)
    }
  }

  //TODO: write to file and not stdout
  def print2Dot(file:String) {
    println(header)
    g.getEdges.foreach(e =>{
      println("%s -> %s".format(nodeId2Str(e.orig), nodeId2Str(e.dest)))
    })
    println(footer)
  }

}
