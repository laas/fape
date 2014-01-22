package planstack.graph

import org.scalatest.{Suite, Spec}
import planstack.graph.core.{Graph, Edge}
import scala.collection.mutable


trait BaseGraphSuite[V,EL,E <: Edge[V]] extends Suite {

  val vertices = (0 to 100).map(x => scala.util.Random.nextInt())
  var nextVert = 0

  def graph : Graph[V,EL,E]

  def newVert(g:Graph[V,EL,E] = graph) : Int = {
    val vert = vertices(nextVert)
    nextVert += 1
    println("New vert : " + vert + "  numVert: "+graph.numVertices)
    vert
  }

}
