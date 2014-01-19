package planstack.graph

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer




abstract class MultiUndirectedAdjacencyList[V,E <: Edge[V]](mEdges : mutable.ArrayBuffer[List[E]],
                                                   mIndexes : mutable.Map[V, Int],
                                                   mVertices : mutable.ArrayBuffer[V])
  extends UndirectedAdjacencyList[V,E](mEdges, mIndexes, mVertices)
  with MultiGraph[V,E]


class MultiLabeledUndirectedAdjacencyList[V, EdgeLabel](mEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                                        mIndexes : mutable.Map[V, Int],
                                                        mVertices : mutable.ArrayBuffer[V])
  extends MultiUndirectedAdjacencyList[V, LabeledEdge[V, EdgeLabel]](mEdges, mIndexes, mVertices)
  with LabeledGraph[V, EdgeLabel]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  def cc() = { new MultiLabeledUndirectedAdjacencyList[V, EdgeLabel](mEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}
