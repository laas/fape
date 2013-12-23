package planstack.graph


class GenEdge[_EdgePL](val orig:Int, val dest:Int, val pl:_EdgePL) {
  override def toString : String = "(%d, %d, %s)".format(orig, dest, pl)
}



trait GenGraph[_Node, _EdgePL] {
  type Edge = GenEdge[_EdgePL]
  type Graph = GenGraph[_Node, _EdgePL]


  var _numVertices = 0

  def numVertices = _numVertices

  def addVertex() : Int

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

  override def clone() : Graph = { throw new Exception("This is an abstract method Graph.clone") }
}



