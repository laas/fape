package planstack.graph

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer






class SimpleLabeledDirectedAdjacencyList[V, EL](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V,EL]]],
                                               mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V,EL]]],
                                               mIndexes : mutable.Map[V, Int],
                                               mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V, EL, LabeledEdge[V,EL]](mOutEdges, mInEdges, mIndexes, mVertices)
  with LabeledGraph[V, EL]
  with SimpleGraph[V, EL, LabeledEdge[V,EL]]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V,EL]]], new ArrayBuffer[List[LabeledEdge[V,EL]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new SimpleLabeledDirectedAdjacencyList[V, EL](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class MultiLabeledDirectedAdjacencyList[V, EL](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EL]]],
                                              mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EL]]],
                                              mIndexes : mutable.Map[V, Int],
                                              mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V, EL, LabeledEdge[V, EL]](mOutEdges, mInEdges, mIndexes, mVertices)
  with LabeledGraph[V, EL]
  with MultiGraph[V, EL, LabeledEdge[V, EL]]
{
  type E = LabeledEdge[V,EL]

  def this() = this(new ArrayBuffer[List[LabeledEdge[V,EL]]], new ArrayBuffer[List[LabeledEdge[V,EL]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new MultiLabeledDirectedAdjacencyList[V, EL](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class SimpleUnlabeledDirectedAdjacencyList[V](mOutEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                      mInEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                      mIndexes : mutable.Map[V, Int],
                                      mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V, Nothing, Edge[V]](mOutEdges, mInEdges, mIndexes, mVertices)
  with UnlabeledGraph[V]
  with SimpleGraph[V, Nothing, Edge[V]]
{
  def this() = this(new ArrayBuffer[List[Edge[V]]], new ArrayBuffer[List[Edge[V]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new SimpleUnlabeledDirectedAdjacencyList[V](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class MultiUnlabeledDirectedAdjacencyList[V](mOutEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mInEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mIndexes : mutable.Map[V, Int],
                                     mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V, Nothing, Edge[V]](mOutEdges, mInEdges, mIndexes, mVertices)
  with UnlabeledGraph[V]
  with MultiGraph[V, Nothing, Edge[V]]
{
  def this() = this(new ArrayBuffer[List[Edge[V]]], new ArrayBuffer[List[Edge[V]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new MultiUnlabeledDirectedAdjacencyList[V](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}
