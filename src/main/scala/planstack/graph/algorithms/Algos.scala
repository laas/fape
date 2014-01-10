package planstack.graph.algorithms

import planstack.graph._
import scala.collection.immutable



/*
object Algos {
  object Marks extends Enumeration {
    type Marks = Value
    val marked, touched, untouched = Value
  }
  import Marks._

  def topologicalSorting[A,B](g:GenGraph[A,B]) : immutable.Seq[Int] = {
    var topo = List[Int]()
    val marks = Array.fill[Marks](g.numVertices)(untouched)

    def visit(n:Int) {
      if(marks(n) == touched)
        throw new Exception("This is not a DAG")
      if(marks(n) == untouched) {
        marks(n) = touched
        g.outEdges(n).foreach(e => visit(e.dest))
        marks(n) = marked
        topo = n :: topo
      }
    }

    while(!marks.forall(_ != untouched)) {
      val optN = (0 to g.numVertices).find(i => marks(i) == untouched)
      optN match  {
        case Some(n) => visit(n)
        case _ => throw new Exception("Problem, no untouched vertex could be found")
      }
    }

    topo
  }
}

*/