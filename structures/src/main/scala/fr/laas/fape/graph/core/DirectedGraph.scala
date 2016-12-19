package fr.laas.fape.graph.core

import scala.collection.JavaConversions._

trait DirectedGraph[V, EL, E <: Edge[V]] extends Graph[V, EL, E] {

  def edges(u:V, v:V) : Seq[E] = outEdges(u).filter(e => e.v == v)

  def inEdges(v:V) : Seq[E]

  def outEdges(u:V) : Seq[E]

  def inDegree(v:V) = inEdges(v).length

  def outDegree(v:V) = outEdges(v).length

  def parents(v:V) : Set[V] = inEdges(v).map(_.u).toSet

  def jParents(v:V) : java.util.Set[V] = setAsJavaSet(parents(v))

  def children(v:V) : Set[V] = outEdges(v).map(_.v).toSet

  def jChildren(v:V) : java.util.Set[V] = setAsJavaSet(children(v))
}


object DirectedGraph {

  def apply[V]() = new impl.MultiUnlabeledDirectedAdjacencyList[V]()
  def apply[V,EL]() = new impl.MultiLabeledDirectedAdjacencyList[V, EL]()

}