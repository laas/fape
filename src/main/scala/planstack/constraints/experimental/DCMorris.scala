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

class NotDC extends Exception

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
    if (!dominated(e)) {
      edges += e
      inEdges(e.to) += e
      outEdges(e.from) += e
      println("    adding edge: " + e)
    }
  }

  private def dominated(e:Edge): Boolean = {
    e.isInstanceOf[Req] && outEdges(e.from)
      .filter(e2 => e2.to == e.to && e.isInstanceOf[Req])
      .exists(e2 => e2.d <= e.d)
  }


  def determineDC(): Boolean = {
    try {
      val propagated = Buff[Node]()
      for (n <- nodes; if isNegative(n))
        dcBackprop(n, propagated, Nil)
    } catch {
      case e:NotDC => return false
    }
    true
  }

  @throws[NotDC]
  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[Node]): Unit = {
    println("current: "+source)
    val negReq = inEdges(source).filter(e => e.isInstanceOf[Req] && e.d < 0).map(_.asInstanceOf[Req]).toList
    dcBackprop(source, propagated, callHistory, Right(negReq))
    for(e <- inEdges(source) if e.d < 0 && e.isInstanceOf[Upper])
      dcBackprop(source, propagated, callHistory, Left(e.asInstanceOf[Upper]))
    propagated += source
    println("end: "+source)
  }

  @throws[NotDC]
  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[Node], curEdges: Either[Upper, List[Req]]) : Unit = {
    case class QueueElem(n: Node, dist: Int)
    def suitable(e: Edge) = (curEdges, e) match  {
      case (Left(Upper(_, _, _, upLbl)) ,Lower(_, _, _, downLbl)) if upLbl == downLbl => false
      case _ => true
    }

    val visited = MSet[Node]()
    if(callHistory contains source)
      throw new NotDC
    if(propagated contains source)
      return ;
    val queue = Queue[QueueElem]()(Ordering.by(- _.dist))

    val toProp = curEdges match {
      case Left(up) => List(up)
      case Right(reqs) => reqs
    }
    for(e <- toProp)
      queue += QueueElem(e.from, e.d)

    while(queue.nonEmpty) {
      val QueueElem(cur, dist) = queue.dequeue()
      println(s"  dequeue: $cur ($dist)")
      if(!visited.contains(cur)) {
        visited += cur

        if(dist >= 0) {
          addEdge(new Req(cur, source, dist))
        } else {
          if(isNegative(cur))
            dcBackprop(cur, propagated, source :: callHistory)
          for(e <- inEdges(cur) ; if e.d >= 0 && suitable(e))
            queue += QueueElem(e.from, dist + e.d)
        }
      }
    }
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
  val F = 6

  val notDCs = List(
    List(Req(A,B,5), Req(B,A,-6)),
    List(Req(C, B, 3), Req(B, C, -1), Upper(B,A,-5,B), Lower(A,B,1,B)),
    List(Req(C, B, 3), Req(B, C, -1), Upper(B,A,-5,B), Lower(A,B,1,B))
  )

  val DCs = List(
    List(Req(B,C, 5), Req(C,B, -5), Upper(B,A,-5,B), Lower(A,B,1,B))
  )

  def assertNotDC(edges: List[Edge]): Unit = {
    val stnu = new DCMorris()
    for(e <- edges)
      stnu.addEdge(e)
    assert(!stnu.determineDC())
  }
  def assertDC(edges: List[Edge]): Unit = {
    val stnu = new DCMorris()
    for(e <- edges)
      stnu.addEdge(e)
    assert(stnu.determineDC())
  }
//  for(notDC <- notDCs) {
//    assertNotDC(notDC)
//  }
//  for(dc <- DCs) {
//    assertDC(dc)
//  }


//  val stnu = new DCMorris()
//
//  val edges = List(
//    Req(E,B,4),
//    Upper(B, A, -2, B),
//    Lower(A, B, 0, B),
//    Req(B, D, 1),
//    Upper(D, C, -3, D),
//    Lower(C, D, 0, D),
//    Req(D, B, 3),
//    Upper(B, A, -2, B),
//    Lower(A, B, 0, B),
//    Req(B, E, -2)
//  )
//  for(e <- edges)
//    stnu.addEdge(e)
//
//  println()
//
//  val props = Buff[Node]()
//  try {
//    stnu.dcBackprop(E, props, Nil)
//    println("DC")
//  } catch {
//    case e:NotDC => println("Not DC")
//  }


  def makeNonObservable(edges: List[Edge], nodes: List[Node]) : List[Edge] = nodes match {
    case Nil => edges
    case h::tail => makeNonObservable(makeNonObservable(edges, h), tail)
  }

  def makeNonObservable(edges: List[Edge], node: Node) : List[Edge] = {
    val upper = edges.find(e => e.isInstanceOf[Upper] && e.asInstanceOf[Upper].lbl == node).get
    val lower = edges.find(e => e.isInstanceOf[Lower] && e.asInstanceOf[Lower].lbl == node).get

    val l = lower.d
    val u = upper.d
    val src = lower.from

    edges.filter(e => e != upper && e != lower).map {
      case Req(`node`,y,d) => Req(src, y, l+d)
      case Req(x,`node`,d) => Req(x, src, d+u)
      case Lower(`node`, y, d, lbl) => Lower(src, y, l+d, lbl)
      case Upper(x, `node`, d, lbl) => Upper(x, src, u+d, lbl)
      case e => assert(e.from != node && e.to != node); e
    }
  }

  val ex1 = List(
    cont(A,B,0,5), reqs(B,C,20,25), reqs(B,D,20,25), cont(F,E,0,5), reqs(E,C,1,6), reqs(E,D,0,5)
  ).flatten

  println("\nObserve example:")
  assertDC(ex1)

  println("\nTwo non-obs:")
  assertNotDC(makeNonObservable(ex1, List(B,E)))

  println("\nOne non-obs:")
  assertDC(makeNonObservable(ex1, List(E)))
  assertNotDC(makeNonObservable(ex1, List(B)))


  def cont(src:Node, dst: Node, minDur:Int, maxDur:Int) = {
    List(Upper(dst, src, -maxDur, dst), Lower(src, dst, minDur, dst))
  }
  def reqs(src:Node, dst:Node, min:Int, max:Int) = {
    List(Req(src, dst, max), Req(dst, src, -min))
  }
}