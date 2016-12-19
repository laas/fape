package fr.laas.fape.graph.core.impl.matrix

import fr.laas.fape.graph.core.{LabeledEdge, SimpleLabeledDigraph}

/** A full graph of integer.
  *
  * @param capacity current capacity of the graph.
  * @param defaultValue Default value to be given to the edges.
  * @param numVertices Number of vertices in the graph
  * @param matrix The actual matrix of integer, it size should be capacity*capacity
  */
class FullIntIntDigraph (protected var capacity : Int,
                         val defaultValue : Int,
                         var numVertices : Int,
                         protected var matrix : Array[Array[Int]])
  extends SimpleLabeledDigraph[Int, Int]
{
  /** size increment when the matrix needs to grow */
  private final val inc = 10

  def this(initialCapacity:Int, defaultValue:Int) =
    this(initialCapacity, defaultValue, 0, Array.fill[Int](initialCapacity,initialCapacity)(defaultValue))

  def this(default:Int) =
    this(10, default, 0, Array.fill[Int](10,10)(default))

  // init matrix with the given capacity
  if(matrix(0) == null)
    for(i <- 0 until capacity)
      matrix(i) = Array.fill[Int](capacity)(defaultValue)

  def inEdges(v: Int): Seq[LabeledEdge[Int, Int]] =
    for(u <- vertices) yield {
      new LabeledEdge[Int, Int](u, v, edgeValue(u, v))
    }

  def outEdges(u: Int): Seq[LabeledEdge[Int, Int]] =
    for(v <- vertices) yield
      new LabeledEdge[Int, Int](u, v, edgeValue(u, v))

  override def edge(u:Int, v:Int) : Option[LabeledEdge[Int, Int]] = {
    Some(new LabeledEdge[Int, Int](u, v, edgeValue(u,v)))
  }

  def addVertex() :Int = addVertex(numVertices)

  /** Adds a new Vertex to the graph */
  def addVertex(v: Int): Int = {
    assert(numVertices == v, "Each new vertex id should be strictly incremented. If possible use addVertex()")

    if(numVertices == capacity) {
      // need to grow
      val oldMat = matrix
      // creates columns with increased capacity
      matrix = Array.fill[Int](capacity+inc,capacity+inc)(defaultValue)
      // copy old lines into new ones
      for(i <- 0 until capacity)
        Array.copy(oldMat(i), 0, matrix(i), 0, capacity)
      capacity += inc
    }
    numVertices += 1
    numVertices-1
  }

  /** Returns a vertices contained in the graph */
  def vertices: Seq[Int] = 0 until numVertices

  protected def addEdgeImpl(e: LabeledEdge[Int, Int]): Unit = {
    matrix(e.u)(e.v) = e.l
  }

  /** Returns true if the vertices is present in the graph. */
  def contains(v: Int): Boolean = v < numVertices && v >= 0

  def edgeValue(u:Int, v:Int) : Int = matrix(u)(v)

  /** All edges of the graph. */
  def edges(): Seq[LabeledEdge[Int, Int]] = {
    for(u <- vertices ; v <- vertices) yield {
      new LabeledEdge[Int, Int](u, v, edgeValue(u, v))
    }
  }

  /** Removes all edges from u to v in the graph */
  def deleteEdges(u: Int, v: Int) { matrix(u)(v) = defaultValue }

  def deleteEdge(e:LabeledEdge[Int,Int]): Unit = {
    throw new UnsupportedOperationException("Cannot remove an edge by reference in a matrix " +
      "since the reference of the edge is not stored.")
  }

  def cc(): FullIntIntDigraph = {
    val newMatrix = new Array[Array[Int]](capacity)
    for(i <- 0 until capacity)
      newMatrix(i) = matrix(i).clone()
    new FullIntIntDigraph(capacity, defaultValue, numVertices, newMatrix)
  }
}