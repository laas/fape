package fr.laas.fape.graph.core.impl

import fr.laas.fape.graph.core._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class SimpleLabeledDirectedAdjacencyList[V, EL](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V,EL]]],
                                               mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V,EL]]],
                                               mIndexes : mutable.Map[V, Int],
                                               mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V, EL, LabeledEdge[V,EL]](mOutEdges, mInEdges, mIndexes, mVertices)
  with SimpleLabeledDigraph[V, EL]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V,EL]]], new ArrayBuffer[List[LabeledEdge[V,EL]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new SimpleLabeledDirectedAdjacencyList[V, EL](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class MultiLabeledDirectedAdjacencyList[V, EL](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EL]]],
                                              mInEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EL]]],
                                              mIndexes : mutable.Map[V, Int],
                                              mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V, EL, LabeledEdge[V, EL]](mOutEdges, mInEdges, mIndexes, mVertices)
  with MultiLabeledDigraph[V, EL]
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
  with SimpleUnlabeledDigraph[V]
{
  def this() = this(new ArrayBuffer[List[Edge[V]]], new ArrayBuffer[List[Edge[V]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new SimpleUnlabeledDirectedAdjacencyList[V](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}


class MultiUnlabeledDirectedAdjacencyList[V](mOutEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mInEdges : mutable.ArrayBuffer[List[Edge[V]]],
                                     mIndexes : mutable.Map[V, Int],
                                     mVertices : mutable.ArrayBuffer[V])
  extends DirectedAdjacencyList[V, Nothing, Edge[V]](mOutEdges, mInEdges, mIndexes, mVertices)
  with MultiUnlabeledDigraph[V]
{
  def this() = this(new ArrayBuffer[List[Edge[V]]], new ArrayBuffer[List[Edge[V]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  override def cc() = { new MultiUnlabeledDirectedAdjacencyList[V](mOutEdges.clone(), mInEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}
