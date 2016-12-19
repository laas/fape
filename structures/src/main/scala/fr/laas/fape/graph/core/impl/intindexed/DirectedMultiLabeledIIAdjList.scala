package fr.laas.fape.graph.core.impl.intindexed

import fr.laas.fape.graph.core.{LabeledEdge, MultiLabeledDigraph}

import scala.collection.mutable

class DirectedMultiLabeledIIAdjList[EL](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[Int,EL]]],
                                        mInEdges : mutable.ArrayBuffer[List[LabeledEdge[Int,EL]]])
  extends DirectedIIAdjList[EL, LabeledEdge[Int,EL]](mOutEdges, mInEdges)
  with MultiLabeledDigraph[Int, EL]
{
  def this() = this(new mutable.ArrayBuffer[List[LabeledEdge[Int, EL]]](), new mutable.ArrayBuffer[List[LabeledEdge[Int, EL]]]())

  def cc() = new DirectedMultiLabeledIIAdjList[EL](mOutEdges.clone(), mInEdges.clone())
}
