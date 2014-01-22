package planstack.graph.core.impl.intindexed

import planstack.graph.core.{DirectedGraph, Edge}
import scala.collection.mutable

abstract class DirectedIIAdjList[EL, E <: Edge[Int]](val mOutEdges : mutable.ArrayBuffer[List[E]],
                                                  val mInEdges : mutable.ArrayBuffer[List[E]])
  extends DirectedGraph[Int,EL,E] {

  var mNumVertices = mOutEdges.length




  def addVertex() : Int = {
    println("1AddVertex()  "+numVertices)
    val id = mNumVertices
    mInEdges.append(List[E]())
    mOutEdges.append(List[E]())
    mNumVertices += 1
    println("2AddVertex()  "+numVertices)
    id
  }

  /**
   * Warning, this method is just a wrapper around addVertex(). It is added for compatibility with other graphs.
   * Any call on this method with v != numVertices will fail on assertion.
   *
   * Proper call is g.addVertex(g.numVertices)
   * If possible, prefer using addVertex() which return the id of the inserted vertex.
   * @param v
   * @return
   */
  def addVertex(v:Int) : Int = {
    println("Addvertex( id )")
    assert(numVertices == v, "Vertex ids have to be the stricly growing (%s != %s)".format(numVertices, v))
    addVertex()
  }

  def vertices = 0 to numVertices-1

  def addEdge(e:E) { ??? }

  def addEdgeImpl(e:E) {
    mOutEdges(e.u) = e :: mOutEdges(e.u)
    mInEdges(e.v) = e :: mInEdges(e.v)
  }

  def outEdges(u:Int) : Seq[E] = mOutEdges(u)

  def inEdges(v:Int) : Seq[E] = mInEdges(v)

  def deleteEdges(u:Int, v:Int) {
    mOutEdges(u) = mOutEdges(u).filter(edge => edge.v != v)
    mInEdges(v) = mInEdges(v).filter(edge => edge.u != u)
  }

  def edges : Seq[E] = {
    var alledges = List[E]()
    mOutEdges.foreach(edgelist => alledges = alledges ++ edgelist)
    alledges
  }

  def contains(v:Int): Boolean = 0 <= v && v < numVertices

  def numVertices = mNumVertices
}