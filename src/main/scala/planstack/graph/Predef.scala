package planstack.graph


object Predef {

  def Ex(m:String) = throw new Exception(m)

  type DirectedSimpleLabeledGraph[V,EL] = Graph[V,EL,LabeledEdge[V,EL]]  with SimpleGraph[V,EL,LabeledEdge[V,EL]]
                                                                      with DirectedGraph[V,EL,LabeledEdge[V,EL]]
                                                                      with LabeledGraph[V,EL]

  def NewDirectedSimpleLabeledGraph[V, EL] : DirectedSimpleLabeledGraph[V,EL] = new SimpleLabeledDirectedAdjacencyList[V, EL]()
}
