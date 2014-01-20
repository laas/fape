package planstack.graph

import org.scalatest.{Suite, Spec}
import planstack.graph.core.{Graph, Edge}


trait BaseGraphSuite[V,EL,E <: Edge[V]] extends Suite {

  def graph : Graph[V,EL,E]

}
