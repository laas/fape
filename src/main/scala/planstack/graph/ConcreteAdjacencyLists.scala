package planstack.graph

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer




abstract class SimpleDirectedAdjacencyList[V,E <: Edge[V]](mOutEdges : mutable.ArrayBuffer[List[E]],
                                          mInEdges : mutable.ArrayBuffer[List[E]],
                                          mIndexes : mutable.Map[V, Int],
                                          mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V,E](mOutEdges, mInEdges, mIndexes, mVertices)
  with SimpleGraph[V,E]





abstract class MultiDirectedAdjacencyList[V,E <: Edge[V]](mOutEdges : mutable.ArrayBuffer[List[E]],
                                         mInEdges : mutable.ArrayBuffer[List[E]],
                                         mIndexes : mutable.Map[V, Int],
                                         mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V,E](mOutEdges, mInEdges, mIndexes, mVertices)
  with MultiGraph[V,E]






class SimpleLabeledDirectedAdjacencyList[V, EdgeLabel](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                               mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                               mIndexes : mutable.Map[V, Int],
                                               mVertices : mutable.ArrayBuffer[V])
  extends SimpleDirectedAdjacencyList[V, LabeledEdge[V, EdgeLabel]](mOutEdges, mInEdges, mIndexes, mVertices)
  with LabeledGraph[V, EdgeLabel]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new SimpleLabeledDirectedAdjacencyList[V, EdgeLabel](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class MultiLabeledDirectedAdjacencyList[V, EdgeLabel](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                              mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                              mIndexes : mutable.Map[V, Int],
                                              mVertices : mutable.ArrayBuffer[V])
  extends MultiDirectedAdjacencyList[V, LabeledEdge[V, EdgeLabel]](mOutEdges, mInEdges, mIndexes, mVertices)
  with LabeledGraph[V, EdgeLabel]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new MultiLabeledDirectedAdjacencyList[V, EdgeLabel](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class SimpleUnlabeledDirectedAdjacencyList[V](mOutEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                      mInEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                      mIndexes : mutable.Map[V, Int],
                                      mVertices : mutable.ArrayBuffer[V])
  extends SimpleDirectedAdjacencyList[V, Edge[V]](mOutEdges, mInEdges, mIndexes, mVertices)
  with UnlabeledGraph[V]
{
  def this() = this(new ArrayBuffer[List[Edge[V]]], new ArrayBuffer[List[Edge[V]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new SimpleUnlabeledDirectedAdjacencyList[V](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class MultiUnlabeledDirectedAdjacencyList[V](mOutEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mInEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mIndexes : mutable.Map[V, Int],
                                     mVertices : mutable.ArrayBuffer[V])
  extends MultiDirectedAdjacencyList[V, Edge[V]](mOutEdges, mInEdges, mIndexes, mVertices)
  with UnlabeledGraph[V]
{
  def this() = this(new ArrayBuffer[List[Edge[V]]], new ArrayBuffer[List[Edge[V]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new MultiUnlabeledDirectedAdjacencyList[V](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}
