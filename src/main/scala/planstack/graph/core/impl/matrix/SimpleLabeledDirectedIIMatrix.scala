package planstack.graph.core.impl.matrix

import planstack.graph.core.{LabeledEdge, SimpleLabeledDigraph}

import scala.reflect.ClassTag

class SimpleLabeledDirectedIIMatrix[EL >: Null : ClassTag](
    protected var capacity : Int,
    var numVertices : Int,
    protected var matrix : Array[Array[EL]])
  extends SimpleLabeledDigraph[Int, EL]
{
  /** size increment when the matrix needs to grow */
  private final val inc = 10

  def this() = this(
    10,
    0,
    new Array[Array[EL]](10)
  )

  // init matrix with the given capacity
  if(matrix(0) == null)
    for(i <- 0 until capacity)
      matrix(i) = new Array[EL](capacity)

  def inEdges(v: Int): Seq[LabeledEdge[Int, EL]] =
    for(u <- vertices ; if edgeValue(u, v) !=  null) yield {
      new LabeledEdge[Int, EL](u, v, edgeValue(u, v))
    }

  def outEdges(u: Int): Seq[LabeledEdge[Int, EL]] =
    for(v <- vertices ; if edgeValue(u, v) !=  null) yield
      new LabeledEdge[Int, EL](u, v, edgeValue(u, v))

  override def edge(u:Int, v:Int) : Option[LabeledEdge[Int, EL]] = {
    if(edgeValue(u, v) == null)
      None
    else
      Some(new LabeledEdge[Int, EL](u, v, edgeValue(u,v)))
  }

  def addVertex() :Int = addVertex(numVertices)

  /** Adds a new Vertex to the graph */
  def addVertex(v: Int): Int = {
    assert(numVertices == v, "Each new vertex id should be strictly incremented. If possible use addVertex()")

    if(numVertices == capacity) {
      // need to grow
      val oldMat = matrix
      // creates columns with increased capacity
      matrix = new Array[Array[EL]](capacity+inc)
      // create lines with increased capacity
      for(i <- 0 until capacity+inc)
        matrix(i) = new Array[EL](capacity + inc)
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

  protected def addEdgeImpl(e: LabeledEdge[Int, EL]): Unit = {
    matrix(e.u)(e.v) = e.l
  }

  /** Returns true if the vertices is present in the graph. */
  def contains(v: Int): Boolean = v < numVertices && v >= 0

  def edgeValue(u:Int, v:Int) : EL = matrix(u)(v)

  /** All edges of the graph. */
  def edges(): Seq[LabeledEdge[Int, EL]] = {
    for(u <- vertices ; v <- vertices ; if edgeValue(u, v) !=  null) yield {
      new LabeledEdge[Int, EL](u, v, edgeValue(u, v))
    }
  }

  /** Removes all edges from u to v in the graph */
  def deleteEdges(u: Int, v: Int) { matrix(u)(v) = null }

  def deleteEdge(e:LabeledEdge[Int,EL]): Unit = {
    throw new UnsupportedOperationException("Cannot remove an edge by reference in a matrix " +
      "since the reference of the edge is not stored.")
  }

  def cc(): SimpleLabeledDirectedIIMatrix[EL] = {
    val newMatrix = new Array[Array[EL]](capacity)
    for(i <- 0 until capacity)
      newMatrix(i) = matrix(i).clone()
    new SimpleLabeledDirectedIIMatrix[EL](capacity, numVertices, newMatrix)
  }
}
