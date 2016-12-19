package fr.laas.fape.graph.core.impl.intindexed

import fr.laas.fape.graph.core.{LabeledEdge, SimpleLabeledDigraph}

import scala.collection.mutable

class DirectedSimpleLabeledIIAdjList[EL](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[Int,EL]]],
                                        mInEdges : mutable.ArrayBuffer[List[LabeledEdge[Int,EL]]])
  extends DirectedIIAdjList[EL, LabeledEdge[Int,EL]](mOutEdges, mInEdges)
  with SimpleLabeledDigraph[Int, EL]
{
  def this() = this(new mutable.ArrayBuffer[List[LabeledEdge[Int, EL]]](), new mutable.ArrayBuffer[List[LabeledEdge[Int, EL]]]())

  def cc() = new DirectedSimpleLabeledIIAdjList[EL](mOutEdges.clone(), mInEdges.clone())
}
