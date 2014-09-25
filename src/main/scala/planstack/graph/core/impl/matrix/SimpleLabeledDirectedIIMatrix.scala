package planstack.graph.core.impl.matrix

import planstack.graph.core.{LabeledEdge, SimpleLabeledDigraph}

import scala.reflect.ClassTag

class SimpleLabeledDirectedIIMatrix[EL >: Null : ClassTag](
    var capacity : Int,
    var numVertices : Int,
    var matrix : Array[Array[EL]])
  extends SimpleLabeledDigraph[Int, EL] {

  def this() = this(
    153,
    0,
    new Array[Array[EL]](153)
  )

//  var capacity = 10
//  var numVertices = 0
//
//  var matrix = new Array[Array[EL]](capacity)
  if(matrix(0) == null)
    for(i <- 0 until capacity)
      matrix(i) = new Array[EL](capacity)

  assert(matrix(0)(0) == null)

  def inEdges(v: Int): Seq[LabeledEdge[Int, EL]] =
    for(u <- vertices ; if edgeValue(u, v) !=  null) yield {
      new LabeledEdge[Int, EL](u, v, edgeValue(u, v))
    }

  def outEdges(u: Int): Seq[LabeledEdge[Int, EL]] =
    for(v <- vertices ; if edgeValue(u, v) !=  null) yield {
      new LabeledEdge[Int, EL](u, v, edgeValue(u, v))
    }

  override def edge(u:Int, v:Int) : Option[LabeledEdge[Int, EL]] = {
    if(edgeValue(u, v) == null)
      None
    else
      Some(new LabeledEdge[Int, EL](u, v, edgeValue(u,v)))
  }

  def addVertex() :Int = addVertex(numVertices)

  /** Adds a new Vertex to the graph */
  def addVertex(v: Int): Int = {
    assert(numVertices == v)
    if(numVertices < capacity)
      numVertices += 1
    else
      throw new RuntimeException("need to grow")
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
