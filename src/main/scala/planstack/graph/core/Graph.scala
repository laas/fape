package planstack.graph.core

import scala.collection.JavaConversions._
import planstack.graph.printers.{GraphDotPrinter, NodeEdgePrinter}

/** Base Trait defining a graph.
  *
  * This is the base trait of this library. If possible, all algorithms should be set to work on this trait.
  *
  * @tparam V Type of vertices
  * @tparam EL Type of the edges' labels. Should be set to Nothing if the graph has no labels on its edges.
  * @tparam E Type of the edges. Should be either Edge[V] or LabeledEdge[V, EL]
  */
trait Graph[V, +EL, E <: Edge[V]] {

  /** Adds a new Vertex to the graph */
  def addVertex(v:V) : Int

  /** Returns a vertices contained in the graph */
  def vertices : Seq[V]

  /** Returns all nodes/vertices in the graph */
  def jVertices = seqAsJavaList(vertices)

  /** Inserts a new edge in the graph. Source and target vertices must be
    * Present in the graph when invoking this method.
    * @param e Edge to insert.
    */
  def addEdge(e:E)

  protected def addEdgeImpl(e:E)

  /** Returns true if the vertices is present in the graph. */
  def contains(v:V) : Boolean

  /** All edges of the graph. */
  def edges() : Seq[E]

  /** All edges of the graph */
  def jEdges = seqAsJavaList(edges())

  /** All edges from u to v.
    * Note that implementation might be dependent on the type of the graph (for instance,
    * it is different between directed and undirected graphs.
    */
  def edges(u:V, v:V) : Seq[E]

  /** Number of vertices in the graph */
  def numVertices : Int

  /** Removes all edges from u to v in the graph */
  def deleteEdges(u:V, v:V)

  /** Builds a new copy of the graph. */
  def cc : Graph[V, EL, E]

  /** Creates a DOT export of the graph and write it to a file
    *
    * @param fileName Path of the file to write it to. If a file is already present, it will be overwritten.
    */
  def exportToDotFile(fileName: String) { new GraphDotPrinter(this).print2Dot(fileName) }

  def exportToDotFile[EdgeLabel >: EL](fileName :String, customPrinter :NodeEdgePrinter[V,EdgeLabel,E]) {
    new GraphDotPrinter(this, customPrinter).print2Dot(fileName)
  }
}




object Graph {

  def apply[V]() = new impl.MultiUnlabeledDirectedAdjacencyList[V]()
  def apply[V, EdgeLabel]() = new impl.MultiLabeledDirectedAdjacencyList[V, EdgeLabel]()

}
