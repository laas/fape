package planstack.constraints.experimental

import scala.collection.mutable.{HashMap => MMap}
import scala.collection.mutable.{ArrayBuffer => Buff}
import scala.collection.mutable.{PriorityQueue => Queue}
import scala.collection.mutable.{Set => MSet}

object DCMorris {
  type Node = Int
  sealed abstract class Edge(val from: Node, val to: Node, val d: Int)
  case class Req(override val from: Node, override val to: Node, override val d: Int) extends Edge(from, to, d)
  case class Upper(override val from: Node, override val to: Node, override val d: Int, lbl: Node) extends Edge(from, to, d)
  case class Lower(override val from: Node, override val to: Node, override val d: Int, lbl: Node) extends Edge(from, to, d)

}

class DCMorris {
  import DCMorris._
  val infty = 999999999



  val edges = Buff[Edge]()
  val inEdges = MMap[Node, Buff[Edge]]()
  val outEdges = MMap[Node, Buff[Edge]]()

  private def ensureSpaceForNode(n: Node): Unit = {
    if(!inEdges.contains(n))
      inEdges += ((n, Buff()))
    if(!outEdges.contains(n))
      outEdges += ((n, Buff()))
  }

  def addEdge(e: Edge): Unit = {
    ensureSpaceForNode(e.from)
    ensureSpaceForNode(e.to)
    edges += e
    inEdges(e.to) += e
    outEdges(e.from) += e
  }


  def determineDC(): Boolean = {
    val propagated = Buff[Node]()
    for(n <- nodes ; if isNegative(n))
      if(!dcBackprop(n, propagated, Nil))
        return false
    true
  }

  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[Node]) : Boolean = {
    case class QueueElem(n: Node, dist: Int)
    def suitable(e: Edge) = e match  {
      case Lower(_, _, _, lbl) if lbl == source => false
      case _ => true
    }

    val visited = MSet[Node]()
    if(callHistory contains source)
      return false
    if(propagated contains source)
      return true
    val queue = Queue[QueueElem]()(Ordering.by(_.dist))

    for(e <- inEdges(source))
      queue += QueueElem(e.from, e.d)

    while(queue.nonEmpty) {
      val QueueElem(cur, dist) = queue.dequeue()
      if(!visited.contains(cur)) {
        visited += cur

        if(dist > 0) {
          addEdge(new Req(cur, source, dist))
        } else {
          if(isNegative(cur))
            if(!dcBackprop(cur, propagated, source :: callHistory))
              return false
          for(e <- inEdges(cur) ; if e.d >= 0 && suitable(e))
            queue += QueueElem(e.from, dist + e.d)
        }
      }
    }
    true
  }




  private def nodes = inEdges.keys
  private def isNegative(n: Node) : Boolean = inEdges(n).exists(e => e.d < 0)



}

object DCMorrisTest extends App {
  import DCMorris._
  val A = 1
  val B = 2
  val C = 3
  val D = 4
  val E: Node = 5

  val stnu = new DCMorris()

  val edges = List(
//    Req(E,B,4),
    Upper(B, A, -2, B),
    Lower(A, B, 0, B)
//    Req(B, D, 1),
//    Upper(D, C, -3, D),
//    Lower(C, D, 0, D),
//    Req(D, B, 3),
//    Upper(B, A, -2, B),
//    Lower(A, B, 0, B),
//    Req(B, E, -5)
  )
  for(e <- edges)
    stnu.addEdge(e)

  println(stnu.determineDC())
}