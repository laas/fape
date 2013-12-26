package planstack.graph

import scala.collection.mutable.ArrayBuffer
import planstack.graph.printers.GraphPrinter


class GenEdge[_EdgePL](val orig:Int, val dest:Int, val pl:_EdgePL) {
  override def toString : String = "(%d, %d, %s)".format(orig, dest, pl)
}



trait GenGraph[_Node, _EdgePL] {
  type Edge = GenEdge[_EdgePL]
  type Graph = GenGraph[_Node, _EdgePL]


  var mNumVertices = 0

  def numVertices = mNumVertices

  def addVertex() : Int = ???

  /** Creates a new edge from v1 to v2 with payload pl
    *
    * Any already existing edge is overwritten.
    * @param v1
    * @param v2
    * @param pl
    * @return
    */
  def setEdge(v1:Int, v2:Int, pl:_EdgePL) : Boolean

  def getPayload(v1:Int, v2:Int) : Option[_EdgePL]

  def outEdges(v:Int) : Seq[Edge]
  def inEdges(v:Int) : Seq[Edge]
  def getEdges : Seq[Edge]

  // TODO: Make that an external aspect
  def dotPrinter() = new GraphPrinter[_Node, _EdgePL](this)

  override def clone() : Graph = { throw new Exception("This is an abstract method Graph.clone") }
}

trait InGraphVerticesGenGraph[_Node, _EdgePL] extends GenGraph[_Node, _EdgePL] {

  var mVertices = new ArrayBuffer[_Node](0)

  def addVertex(v:_Node) : Int = {
    mVertices.append(v)
    super.addVertex()
  }
  def vertex(id:Int) : _Node = { mVertices(id) }

  override def clone() : Graph = {
    val newGraph = super.clone().asInstanceOf[InGraphVerticesGenGraph[_Node, _EdgePL]]
    newGraph.mVertices = this.mVertices.clone()
    newGraph
  }
}



