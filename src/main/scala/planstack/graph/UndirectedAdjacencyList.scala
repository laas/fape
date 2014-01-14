package planstack.graph

import scala.collection.mutable

class UndirectedAdjacencyList[V,E <: Edge[V]](val mEdges : mutable.ArrayBuffer[List[E]],
                                              val mIndexes : mutable.Map[V, Int],
                                              val mVertices : mutable.ArrayBuffer[V])
  extends UndirectedGraph[V,E] {


  var mNumVertices = mVertices.length

  def this() = this(new mutable.ArrayBuffer[List[E]](0),
    mutable.Map[V, Int](), new mutable.ArrayBuffer[V](0))

  def addVertex(v: V): Int = {
    assert(!contains(v), "This graph already contains this vertex: " + v)

    val vertId = this.numVertices
    mEdges.append(List[E]())
    mVertices.append(v)
    mIndexes.+=((v, vertId))

    mNumVertices += 1
    return vertId
  }

  def vertices = mVertices.toSeq

  /** Changes a vertex to another object.
    * The new vertex _must_ have the same hash code and be equal to the previous one.
    * @param v
    */
  def updateVertex(v:V) {
    assert(contains(v))
    val id = mIndexes(v)
    mVertices(id) = v
  }

  def addEdge(e: E): Unit = ???

  protected def addEdgeImpl(e: E): Unit = {
    val uId = mIndexes(e.u)
    val vId = mIndexes(e.v)

    mEdges(uId) = e :: mEdges(uId)
    mEdges(vId) = e :: mEdges(vId)
  }

  def contains(v: V): Boolean = mIndexes.contains(v)

  /**
   * Return all edges in the graph.
   * @return
   */
  def edges: Seq[E] = {
    // Here edges are indexed twice so we take only those that start with the current vertex
    for(vertId <- 0 to mNumVertices - 1 ; vert = mVertices(vertId) ;
        e <- mEdges(vertId) ;
        if(e.u == vert)) yield e
  }

  def numVertices: Int = mNumVertices

  /**
   * Removes all edges (u, v) and (v, u) in the graph
   * @param u
   * @param v
   */
  def deleteEdges(u: V, v: V): Unit = {
    val uId = mIndexes(u)
    val vId = mIndexes(v)
    mEdges(uId) = mEdges(uId).filter(edge => edge.v != v && edge.u != v)
    mEdges(vId) = mEdges(vId).filter(edge => edge.u != u && edge.v != u)
  }

  /**
   * Returns all edges touching v
   * @param v
   * @return
   */
  def edges(v: V): Seq[E] = mEdges(mIndexes(v))
}
