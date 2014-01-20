package planstack.graph

import org.scalatest.{Suite, Spec}


trait BaseGraphSuite[V,EL,E <: Edge[V]] extends Suite {

  def graph : Graph[V,EL,E]

}
