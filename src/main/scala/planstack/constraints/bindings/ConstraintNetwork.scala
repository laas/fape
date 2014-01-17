package planstack.constraints.bindings

import scala.collection.mutable
import planstack.graph.UndirectedGraph
import planstack.constraints._

class ConstraintNetwork {

  val node = mutable.Map[Int, CSPVar]()
  val cn = UndirectedGraph[Int, BinaryConstraint]()

  def newVar(dom : Set[Int]) = {
    val cspVar = new CSPVar(cn.numVertices, dom)
    cn.addVertex(cspVar.id)
    node += ((cspVar.id,  cspVar))
    cspVar
  }

  def AC3() : Boolean = {
    var worklist = mutable.Set[Pair[Int,Int]]()

    for(e <- cn.edges() ; c = e.e) {
      worklist += ((c.x, c.y), (c.y, c.x))
    }

    while(worklist.nonEmpty) {
      val cur = worklist.head
      worklist = worklist.tail
      println(cur)
      if(arcReduce(cur._1, cur._2)) {
        if(node(cur._1).dom.isEmpty) {
          return false
        } else {
          for(e <- cn.edges(cn.mVertices(cur._1)) ; c = e.e ; if(c.x != cur._2 && c.y != cur._2)) {
            worklist += ((c.x, c.y), (c.y, c.x))
          }
        }
      }
    }
    return true
  }

  def addConstraint(c:Constraint) {
    c match {
      case bc:BinaryConstraint => cn.addEdge(cn.mVertices(bc.x), cn.mVertices(bc.y), bc)
      case hv:EqValue => node(hv.x) = node(hv.x).setVal(hv.value)
      case nv:DiffValue => {
        assert(node(nv.x).remove(nv.value) == node(nv.x))
        node(nv.x) = node(nv.x).remove(nv.value)
      }
    }
  }

  def arcReduce(x:Int, y:Int):Boolean = {
    assert(containsVertex(x))
    assert(containsVertex(y))
    val xVar = node(x)
    val yVar = node(y)
    val constraints = cn.edges(x, y).map(_.e)

    var change = false
    for(xValue <- xVar.dom) {
      var possible = false
      for(yValue <- yVar.dom) {
        possible = possible || constraints.forall(c => c.isSatisfying(xValue, yValue))
        println("%s (%d %d)".format(possible, xValue, yValue))
      }

      if(!possible) {
        println("%s  -->  %s".format(xVar, xVar.remove(xValue)))
        node(x) = node(x).remove(xValue)
        change = true
      }

    }

    println("-- Change: "+change)
    cn.mVertices.map(node(_)).foreach(println(_))
    change
  }

  def containsVertex(x:Int) = 0 <= x &&  x < cn.numVertices

  def print() { cn.mVertices.map(node(_)).foreach(println(_)) }

}
