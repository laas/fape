package planstack.graph

import org.scalatest.FunSuite
import planstack.graph.core.{Edge, Graph}


trait BaseGraphSuite[V,EL,E <: Edge[V]] extends FunSuite {

  val vertices = (0 to 100).map(x => scala.util.Random.nextInt())
  var nextVert = 0

  def graph : Graph[V,EL,E]

  def newVert(g:Graph[V,EL,E] = graph) : Int = {
    val vert = vertices(nextVert)
    nextVert += 1
    vert
  }

}
