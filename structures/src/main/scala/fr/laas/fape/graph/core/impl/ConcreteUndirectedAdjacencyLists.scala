package fr.laas.fape.graph.core.impl

import fr.laas.fape.graph.core.{LabeledEdge, LabeledGraph, MultiGraph}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class MultiLabeledUndirectedAdjacencyList[V, EdgeLabel](mEdges : mutable.ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]],
                                                        mIndexes : mutable.Map[V, Int],
                                                        mVertices : mutable.ArrayBuffer[V])
  extends UndirectedAdjacencyList[V, EdgeLabel, LabeledEdge[V, EdgeLabel]](mEdges, mIndexes, mVertices)
  with LabeledGraph[V, EdgeLabel]
  with MultiGraph[V, EdgeLabel, LabeledEdge[V,EdgeLabel]]
{
  def this() = this(new ArrayBuffer[List[LabeledEdge[V, EdgeLabel]]], mutable.Map[V,Int](), new ArrayBuffer[V])

  def cc = { new MultiLabeledUndirectedAdjacencyList[V, EdgeLabel](mEdges.clone(), mIndexes.clone(), mVertices.clone()) }
}
