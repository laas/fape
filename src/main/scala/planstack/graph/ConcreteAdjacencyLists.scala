package planstack.graph

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer




class SimpleAdjacencyList[V,E <: Edge[V]](mOutEdges : mutable.ArrayBuffer[List[E]],
                                          mInEdges : mutable.ArrayBuffer[List[E]],
                                          mIndexes : mutable.Map[V, Int],
                                          mVertices : mutable.ArrayBuffer[V])
  extends AdjacencyList[V,E](mOutEdges, mInEdges, mIndexes, mVertices)
  with SimpleGraph[V,E]





class MultiAdjacencyList[V,E <: Edge[V]](mOutEdges : mutable.ArrayBuffer[List[E]],
                                         mInEdges : mutable.ArrayBuffer[List[E]],
                                         mIndexes : mutable.Map[V, Int],
                                         mVertices : mutable.ArrayBuffer[V])
  extends AdjacencyList[V,E](mOutEdges, mInEdges, mIndexes, mVertices)
  with MultiGraph[V,E]






class SimpleLabeledAdjacencyList[V, EdgeLabel](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                               mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                               mIndexes : mutable.Map[V, Int],
                                               mVertices : mutable.ArrayBuffer[V])
  extends SimpleAdjacencyList[V, LabeledEdge[V, EdgeLabel]](mOutEdges, mInEdges, mIndexes, mVertices)
  with LabeledGraph[V, EdgeLabel]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def clone() = { new SimpleLabeledAdjacencyList[V, EdgeLabel](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class MultiLabeledAdjacencyList[V, EdgeLabel](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                              mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                              mIndexes : mutable.Map[V, Int],
                                              mVertices : mutable.ArrayBuffer[V])
  extends MultiAdjacencyList[V, LabeledEdge[V, EdgeLabel]](mOutEdges, mInEdges, mIndexes, mVertices)
  with LabeledGraph[V, EdgeLabel]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def clone() = { new MultiLabeledAdjacencyList[V, EdgeLabel](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class SimpleUnlabeledAdjacencyList[V](mOutEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                      mInEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                      mIndexes : mutable.Map[V, Int],
                                      mVertices : mutable.ArrayBuffer[V])
  extends SimpleAdjacencyList[V, Edge[V]](mOutEdges, mInEdges, mIndexes, mVertices)
  with UnlabeledGraph[V]



class MultiUnlabeledAdjacencyList[V](mOutEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mInEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mIndexes : mutable.Map[V, Int],
                                     mVertices : mutable.ArrayBuffer[V])
  extends MultiAdjacencyList[V, Edge[V]](mOutEdges, mInEdges, mIndexes, mVertices)
  with UnlabeledGraph[V]
{
  def this() = this(new ArrayBuffer[List[Edge[V]]], new ArrayBuffer[List[Edge[V]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def clone() = { new MultiUnlabeledAdjacencyList[V](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}
