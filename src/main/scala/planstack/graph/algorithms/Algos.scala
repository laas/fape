package planstack.graph.algorithms

import planstack.graph._
import scala.collection.mutable




object Algos {
  object Marks extends Enumeration {
    type Marks = Value
    val marked, touched, untouched = Value
  }
  import Marks._

  def topologicalSorting[A,B <: Edge[A]](g:DirectedGraph[A,B]) : Seq[A] = {
    var topo = List[A]()
    val marksMap = mutable.Map[A, Marks]()

    def mark(a:A) = marksMap.getOrElse(a, untouched)

    def visit(n:A) {
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

