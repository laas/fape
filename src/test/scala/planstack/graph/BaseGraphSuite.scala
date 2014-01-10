package planstack.graph

import org.scalatest.{Suite, Spec}


trait BaseGraphSuite[V,E <: Edge[V]] extends Suite {

  def graph : Graph[V,E]

}
