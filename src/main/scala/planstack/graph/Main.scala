package planstack.graph

import planstack.graph.printers.GraphDotPrinter

object Main {

  val g = Graph[String]()

  g.addVertex("Base")
  g.addVertex("Bot1")
  g.addVertex("Bot2")
  g.addEdge("Base", "Bot1")
  g.addEdge("Base", "Bot2")

  val printer = new GraphDotPrinter[String, Edge[String]](g)
  printer.print2Dot("/home/abitmonn/tmp/g.dot")

}
