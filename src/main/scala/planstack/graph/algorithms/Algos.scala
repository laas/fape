package planstack.graph.algorithms

import planstack.graph._
import scala.collection.mutable




object Algos {
  object Marks extends Enumeration {
    type Marks = Value
    val marked, touched, untouched = Value
  }
  import Marks._

  def topologicalSorting[V,EL,E <: Edge[V]](g:DirectedGraph[V,EL,E]) : Seq[V] = {
    var topo = List[V]()
    val marksMap = mutable.Map[V, Marks]()

    def mark(a:V) = marksMap.getOrElse(a, untouched)

    def visit(n:V) {
      if(mark(n) == touched)
        throw new Exception("This is not a DAG")
      if(mark(n) == untouched) {
        marksMap(n) = touched
        g.outEdges(n).foreach(e => visit(e.v))
        marksMap(n) = marked
        topo = n :: topo
      }
    }

    while(!g.vertices().forall(mark(_) != untouched)) {
      val optN = g.vertices().find(i => mark(i) == untouched)
      optN match  {
        case Some(n) => visit(n)
        case _ => throw new Exception("Problem, no untouched vertex could be found")
      }
    }

    topo
  }
}

