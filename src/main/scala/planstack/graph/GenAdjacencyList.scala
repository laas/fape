package planstack.graph


import scala.collection.mutable.ArrayBuffer







class GenAdjacencyList[_Node, _EdgePL](val _outedges :ArrayBuffer[List[GenEdge[_EdgePL]]],
                                       val _inedges :ArrayBuffer[List[GenEdge[_EdgePL]]])
                                       extends GenGraph[_Node, _EdgePL] {

  type AdjacencyList = GenAdjacencyList[_Node, _EdgePL]

  _numVertices = _outedges.length

  def this() = this(new ArrayBuffer[List[GenEdge[_EdgePL]]](0), new ArrayBuffer[List[GenEdge[_EdgePL]]](0))

  //val inedges = new ArrayBuffer[List[Edge]](0)
  //val outedges = new ArrayBuffer[List[Edge]](0)

  def addVertex() : Int = {
    _inedges.append(List[Edge]())
    _outedges.append(List[Edge]())
    _numVertices += 1
    return _numVertices - 1
  }

  def setEdge(v1:Int, v2:Int, w:_EdgePL) : Boolean = {
    val newEdge = new Edge(v1, v2, w)

    _outedges(v1) = newEdge :: _outedges(v1).filter(e => e.dest != v2)
    _inedges(v2) = newEdge :: _inedges(v2).filter(e => e.orig != v1)

    true
  }

  def getPayload(v1:Int, v2:Int) : Option[_EdgePL] = {
    val opt = _outedges(v1).find(e => e.dest == v2)
    opt match {
      case Some(e) => Some(e.pl)
      case _ => None
    }
  }

  def outEdges(v:Int) : Seq[Edge] = _outedges(v)
  def inEdges(v:Int) : Seq[Edge] = _inedges(v)
  def getEdges : Seq[Edge] = {
    var alledges = List[Edge]()
    _outedges.foreach(edgelist => alledges = alledges ++ edgelist)
    alledges
  }

  override def clone() : AdjacencyList = {
    new AdjacencyList(_outedges.clone(), _inedges.clone())
  }
}