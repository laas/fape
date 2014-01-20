package planstack.graph.core.impl

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import planstack.graph.core.{MultiGraph, LabeledEdge, LabeledGraph}
import planstack.graph.core.impl.UndirectedAdjacencyList


class MultiLabeledUndirectedAdjacencyList[V, EdgeLabel](mEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                                        mIndexes : mutable.Map[V, Int],
                                                        mVertices : mutable.ArrayBuffer[V])
  extends UndirectedAdjacencyList[V, EdgeLabel, LabeledEdge[V, EdgeLabel]](mEdges, mIndexes, mVertices)
  with LabeledGraph[V, EdgeLabel]
  with MultiGraph[V, EdgeLabel, LabeledEdge[V,EdgeLabel]]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  def cc() = { new MultiLabeledUndirectedAdjacencyList[V, EdgeLabel](mEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}
