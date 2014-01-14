package planstack.graph

import scala.collection.mutable

class DirectedAdjacencyList[V,E <: Edge[V]](val mOutEdges : mutable.ArrayBuffer[List[E]],
                                    val mInEdges : mutable.ArrayBuffer[List[E]],
                                    val mIndexes : mutable.Map[V, Int],
                                    val mVertices : mutable.ArrayBuffer[V])
  extends DirectedGraph[V,E] {

  var mNumVertices = mOutEdges.length

  def this() = this(new mutable.ArrayBuffer[List[E]](0), new mutable.ArrayBuffer[List[E]](0),
                    mutable.Map[V, Int](), new mutable.ArrayBuffer[V](0))



  def addVertex(v:V) : Int = {
    assert(!contains(v))

    val vertId = this.numVertices
    mInEdges.append(List[E]())
    mOutEdges.append(List[E]())
    mVertices.append(v)
    mIndexes.+=((v, vertId))

    mNumVertices += 1
    return vertId
  }

  def vertices = mVertices.toSeq

  def addEdge(e:E) { ??? }

  def addEdgeImpl(e:E) {
    val uId = mIndexes(e.u)
    val vId = mIndexes(e.v)

    mOutEdges(uId) = e :: mOutEdges(uId)
    mInEdges(vId) = e :: mInEdges(vId)
  }

  def outEdges(u:V) : Seq[E] = mOutEdges(mIndexes(u))

  def inEdges(v:V) : Seq[E] = mInEdges(mIndexes(v))

  def deleteEdges(u:V, v:V) {
    val uId = mIndexes(u)
    val vId = mIndexes(v)
    mOutEdges(uId) = mOutEdges(uId).filter(edge => edge.v != v)
    mInEdges(vId) = mInEdges(vId).filter(edge => edge.u != u)
  }

  def edges : Seq[E] = {
    var alledges = List[E]()
    mOutEdges.foreach(edgelist => alledges = alledges ++ edgelist)
    alledges
  }

  override def clone() : DirectedAdjacencyList[V,E] = {
    new DirectedAdjacencyList[V,E](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone())
  }

  def contains(v: V): Boolean = mIndexes.contains(v)

  def numVertices = mNumVertices
}
