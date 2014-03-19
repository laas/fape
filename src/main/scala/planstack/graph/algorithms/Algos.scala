package planstack.graph.algorithms

import planstack.graph._
import scala.collection.mutable
import planstack.graph.core.{LabeledEdge, LabeledDigraph, Edge, DirectedGraph}


object Algos {
  object Marks extends Enumeration {
    type Marks = Value
    val marked, touched, untouched = Value
  }
  import Marks._

  def topologicalSorting[V,EL,E <: Edge[V]](g:DirectedGraph[V,EL,E]) : List[V] = {
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

    while(!g.vertices.forall(mark(_) != untouched)) {
      val optN = g.vertices.find(i => mark(i) == untouched)
      optN match  {
        case Some(n) => visit(n)
        case _ => throw new Exception("Problem, no untouched vertex could be found")
      }
    }

    topo
  }


  def depthFirstOrderReversePost[V,EL,E <: Edge[V]](g:DirectedGraph[V,EL,E]) : Seq[V] = {
    val stack = new mutable.Stack[V]()

    val marked = new mutable.HashSet[V]()

    def dfs(v:V) {
      marked += v
      for(child <- g.children(v)) {
        if(!marked.contains(child))
          dfs(child)
      }
      stack.push(v)
    }

    for(v <- g.vertices) {
      if(!marked.contains(v))
        dfs(v)
    }

    return stack.toList
  }

  //TODO clean up
  def getNegativeCycle[V](g : LabeledDigraph[V, Int]) {
    val distances = mutable.Map[V, Int]()
    val predecessors = mutable.Map[V,V]()
    //val cycles = Set(Seq[LabeledEdge[V,Int]])

    for(v <- g.vertices) {
      distances(v) = 9999999
    }
    distances(g.vertices.head) = 0

    var numIter = 0
    while(numIter < g.numVertices) {
      for(e <- g.edges()) {
        if(distances(e.v) > distances(e.u) +e.l) {
          distances(e.v) = distances(e.u) + e.l
          predecessors(e.v) = e.u
        }
      }
      numIter += 1
    }
    val hasNegativeCycle = !g.edges().forall(e => distances(e.v) >= distances(e.u) +e.l)

    println(predecessors)

    if(hasNegativeCycle) {
      val label = mutable.Map[V, Int]()
      for(v <- g.vertices)
        label(v) = 0

      def dfs(v:V, cycle : List[V] = List()) : List[V] = {
        if(label(v) == 0) {
          label(v) = 1
          if(predecessors.contains(v))
            dfs(predecessors(v), cycle)
          else
            cycle
        } else if(label(v) == 1) {
          //already visited that
          label(v) = 2

          //append v to the cycle and keep searching
          if(predecessors.contains(v))
            dfs(predecessors(v), v :: cycle)
          else
            v :: cycle
        } else {
          cycle
        }
      }

      for(v <- g.vertices) {
        if(label(v) == 0) {
          dfs(v) match {
            case Nil =>
            case l => {
              val cycle = l.toVector

              val edgesOfCycle =
                for(i <- 0 to cycle.size-1) yield {
                  println(i)
                  println()
                  g.edges(cycle(i), cycle((i+1) % cycle.size))
                }
              println(edgesOfCycle)
            }
          }


          for(vv <- g.vertices ; if(label(vv) == 1)) {
            label(vv) = 2
          }
        }
      }
    }


  }
}

