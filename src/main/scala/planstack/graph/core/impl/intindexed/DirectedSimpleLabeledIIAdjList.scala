package planstack.graph.core.impl.intindexed

import planstack.graph.core._
import scala.collection.mutable

class DirectedSimpleLabeledIIAdjList[EL](mOutEdges : mutable.ArrayBuffer[List[LabeledEdge[Int,EL]]],
                                        mInEdges : mutable.ArrayBuffer[List[LabeledEdge[Int,EL]]])
  extends DirectedIIAdjList[EL, LabeledEdge[Int,EL]](mOutEdges, mInEdges)
  with SimpleLabeledDigraph[Int, EL]
{
  def this() = this(new mutable.ArrayBuffer[List[LabeledEdge[Int, EL]]](), new mutable.ArrayBuffer[List[LabeledEdge[Int, EL]]]())

  def cc() = new DirectedSimpleLabeledIIAdjList[EL](mOutEdges.clone(), mInEdges.clone())
}
