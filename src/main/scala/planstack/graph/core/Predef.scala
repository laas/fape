package planstack.graph.core

import planstack.graph.core.impl.{SimpleUnlabeledDirectedAdjacencyList, MultiUnlabeledDirectedAdjacencyList}


trait LabeledDigraph[V,EL] extends Graph[V,EL, LabeledEdge[V,EL]] with LabeledGraph[V,EL] with DirectedGraph[V, EL, LabeledEdge[V,EL]]

trait UnlabeledDigraph[V] extends Graph[V, Nothing, Edge[V]] with UnlabeledGraph[V] with DirectedGraph[V,Nothing,Edge[V]] {
  def cc : UnlabeledDigraph[V]
}

object UnlabeledDigraph {
  def apply[V] = new MultiUnlabeledDirectedAdjacencyList[V]()
}


trait SimpleLabeledDigraph[V,EL] extends LabeledDigraph[V,EL] with SimpleGraph[V, EL, LabeledEdge[V,EL]]
trait MultiLabeledDigraph[V,EL] extends LabeledDigraph[V,EL] with MultiGraph[V, EL, LabeledEdge[V,EL]]


trait SimpleUnlabeledDigraph[V] extends UnlabeledDigraph[V] with SimpleGraph[V, Nothing, Edge[V]]

object SimpleUnlabeledDigraph {
  def apply[V] : SimpleUnlabeledDigraph[V] = new SimpleUnlabeledDirectedAdjacencyList[V]()
}

trait MultiUnlabeledDigraph[V] extends UnlabeledDigraph[V] with MultiGraph[V, Nothing, Edge[V]]