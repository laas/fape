package planstack.graph


import scala.collection.mutable.ArrayBuffer







class GenAdjacencyList[_Node, _EdgePL](val mOutEdges :ArrayBuffer[List[GenEdge[_EdgePL]]],
                                       val mInEdges :ArrayBuffer[List[GenEdge[_EdgePL]]])
                                       extends GenGraph[_Node, _EdgePL] {

  type AdjacencyList = GenAdjacencyList[_Node, _EdgePL]

  mNumVertices = mOutEdges.length

  def this() = this(new ArrayBuffer[List[GenEdge[_EdgePL]]](0), new ArrayBuffer[List[GenEdge[_EdgePL]]](0))

  override def addVertex() : Int = {
    mInEdges.append(List[Edge]())
    mOutEdges.append(List[Edge]())
    mNumVertices += 1
    return mNumVertices - 1
  }

  def setEdge(v1:Int, v2:Int, w:_EdgePL) : Boolean = {
    val newEdge = new Edge(v1, v2, w)

    mOutEdges(v1) = newEdge :: mOutEdges(v1).filter(e => e.dest != v2)
    mInEdges(v2) = newEdge :: mInEdges(v2).filter(e => e.orig != v1)

    true
  }

  def getPayload(v1:Int, v2:Int) : Option[_EdgePL] = {
    val opt = mOutEdges(v1).find(e => e.dest == v2)
    opt match {
      case Some(e) => Some(e.pl)
      case _ => None
    }
  }

  def outEdges(v:Int) : Seq[Edge] = mOutEdges(v)
  def inEdges(v:Int) : Seq[Edge] = mInEdges(v)
  def getEdges : Seq[Edge] = {
    var alledges = List[Edge]()
    mOutEdges.foreach(edgelist => alledges = alledges ++ edgelist)
    alledges
  }

  override def clone() : Graph = {
    new AdjacencyList(mOutEdges.clone(), mInEdges.clone())
  }
}