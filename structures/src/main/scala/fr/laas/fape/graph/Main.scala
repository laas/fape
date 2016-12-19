package fr.laas.fape.graph

import fr.laas.fape.graph.core.Graph
import fr.laas.fape.graph.printers.GraphDotPrinter

object Main {

  val g = Graph[String]()

  g.addVertex("Base")
  g.addVertex("Bot1")
  g.addVertex("Bot2")
  g.addEdge("Base", "Bot1")
  g.addEdge("Base", "Bot2")

  val printer = new GraphDotPrinter(g)
  printer.print2Dot("/home/abitmonn/tmp/g.dot")

}
