package planstack.constraints.experimental

import java.util.Optional

import planstack.anml.model.concrete.{GlobalRef, TPRef}
import planstack.constraints.experimental.DCMorris.Proj
import planstack.constraints.stnu.{ElemStatus, Constraint, PseudoSTNUManager}

import scala.collection.immutable.Set
import scala.collection.mutable.{HashMap => MMap}
import scala.collection.mutable.{ArrayBuffer => Buff}
import scala.collection.mutable.{PriorityQueue => Queue}
import scala.collection.mutable.{Set => MSet}
import scala.language.implicitConversions

import java.{util => ju}
import scala.collection.JavaConverters._

object DCMorris {
  type Node = Int

  sealed abstract class Proj(val n: Node)
  case class MaxProj(override val n: Node) extends Proj(n)
  case class MinProj(override val n: Node) extends Proj(n)

  sealed abstract class Edge(val from: Node, val to: Node, val d: Int, val proj:Set[Proj])
  case class Req(override val from: Node, override val to: Node, override val d: Int, override val proj:Set[Proj])
    extends Edge(from, to, d, proj)
  case class Upper(override val from: Node, override val to: Node, override val d: Int, lbl: Node, override val proj:Set[Proj]) extends Edge(from, to, d, proj)
  case class Lower(override val from: Node, override val to: Node, override val d: Int, lbl: Node, override val proj:Set[Proj]) extends Edge(from, to, d, proj)

  def log(msg: String) = Unit //println(msg)
}

class NotDC(val involvedProjections: Set[Proj]) extends Exception

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
      log("    adding edge: " + e)
    }
  }

  private def dominated(e:Edge): Boolean = {
    e.isInstanceOf[Req] && outEdges(e.from)
      .filter(e2 => e2.to == e.to && e.isInstanceOf[Req])
      .exists(e2 => e2.d <= e.d)
  }

  private def extractNeedObs(projs: Set[Proj]) : Set[Node] = {
    val maxProjs = projs.filter(p => p.isInstanceOf[MaxProj]).map(p => p.n)
    projs.filter(p => p.isInstanceOf[MinProj] && maxProjs.contains(p.n)).map(p => p.n)
  }

  def determineDC(): (Boolean, Option[Set[Node]]) = {
    try {
      val propagated = Buff[Node]()
      for (n <- nodes; if isNegative(n))
        dcBackprop(n, propagated, Nil)
    } catch {
      case e:NotDC => return (false, Some(extractNeedObs(e.involvedProjections)))
    }
    (true, None)
  }

  @throws[NotDC]
  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[Node]): Unit = {
    log("current: "+source)
    val negReq = inEdges(source).filter(e => e.isInstanceOf[Req] && e.d < 0).map(_.asInstanceOf[Req]).toList
    dcBackprop(source, propagated, callHistory, Right(negReq))
    for(e <- inEdges(source) if e.d < 0 && e.isInstanceOf[Upper])
      dcBackprop(source, propagated, callHistory, Left(e.asInstanceOf[Upper]))
    propagated += source
    log("end: "+source)
  }

  @throws[NotDC]
  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[Node], curEdges: Either[Upper, List[Req]]) : Unit = {
    case class QueueElem(n: Node, dist: Int, projs: Set[Proj])
    def suitable(e: Edge) = (curEdges, e) match  {
      case (Left(Upper(_, _, _, upLbl, _)) ,Lower(_, _, _, downLbl, _)) if upLbl == downLbl => false
      case _ => true
    }

    val visited = MSet[Node]()
    assert(!callHistory.contains(source), "This should have been caught earlier")

    if(propagated contains source)
      return ;
    val queue = Queue[QueueElem]()(Ordering.by(- _.dist))

    val toProp = curEdges match {
      case Left(up) => List(up)
      case Right(reqs) => reqs
    }
    for(e <- toProp)
      queue += QueueElem(e.from, e.d, e.proj)

    while(queue.nonEmpty) {
      val QueueElem(cur, dist, projs) = queue.dequeue()
      log(s"  dequeue: $cur ($dist) $projs")
      if(!visited.contains(cur)) {
        visited += cur

        if(dist >= 0) {
          addEdge(new Req(cur, source, dist, projs))
        } else {
          if(isNegative(cur)) {
            if(source == cur || callHistory.contains(cur))
              throw new NotDC(projs)
            dcBackprop(cur, propagated, source :: callHistory)
          }
          for(e <- inEdges(cur) ; if e.d >= 0 && suitable(e))
            queue += QueueElem(e.from, dist + e.d, projs ++ e.proj)
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
    List(Req(A,B,5, Set()), Req(B,A,-6, Set())),
    List(Req(C, B, 3, Set()), Req(B, C, -1, Set()), Upper(B,A,-5,B, Set()), Lower(A,B,1,B, Set())),
    List(Req(C, B, 3, Set()), Req(B, C, -1, Set()), Upper(B,A,-5,B, Set()), Lower(A,B,1,B, Set()))
  )

  val DCs = List(
    List(Req(B,C, 5, Set()), Req(C,B, -5, Set()), Upper(B,A,-5,B, Set()), Lower(A,B,1,B, Set()))
  )

  def assertNotDC(edges: List[Edge]): Unit = {
    val stnu = new DCMorris()
    for(e <- edges)
      stnu.addEdge(e)
    val (dc,obs) = stnu.determineDC()
    assert(!dc)
    log(s"You should observe at least one of events: $obs")
  }
  def assertDC(edges: List[Edge]): Unit = {
    val stnu = new DCMorris()
    for(e <- edges)
      stnu.addEdge(e)
    val (dc,_) = stnu.determineDC()
    assert(dc)
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
//  log()
//
//  val props = Buff[Node]()
//  try {
//    stnu.dcBackprop(E, props, Nil)
//    log("DC")
//  } catch {
//    case e:NotDC => log("Not DC")
//  }

//  def convertFrom(stnu: PseudoSTNUManager) : List[Edge] = {
//    for(n <- stnu.timepoints)
//  }

  case class NeededObservations(resolvingObs: ju.Collection[ju.Set[TPRef]])

  def makePossiblyObservable(edges: List[Edge], nodes: List[Node]) : List[Edge] = nodes match {
    case Nil => edges
    case h::tail => makePossiblyObservable(makePossiblyObservable(edges, h), tail)
  }

  def makePossiblyObservable(edges: List[Edge], node: Node) : List[Edge] = {
    // extract contingent edges on "node"
    val upper = edges.find(e => e.isInstanceOf[Upper] && e.asInstanceOf[Upper].lbl == node).get
    val lower = edges.find(e => e.isInstanceOf[Lower] && e.asInstanceOf[Lower].lbl == node).get

    val l = lower.d
    val u = upper.d
    val src = lower.from

    edges.filter(e => e != upper && e != lower).map {
      case Req(`node`,y,d, projs) => Req(src, y, l+d, projs ++ lower.proj + MinProj(node))
      case Req(x,`node`,d, projs) => Req(x, src, d+u, projs ++ upper.proj + MaxProj(node))
      case Lower(`node`, y, d, lbl, projs) => Lower(src, y, l+d, lbl, projs ++ lower.proj + MinProj(node))
      case Upper(x, `node`, d, lbl, projs) => Upper(x, src, u+d, lbl, projs ++ upper.proj + MaxProj(node))
      case e => assert(e.from != node && e.to != node); e
    }
  }
  def makeNonObservable(edges: List[Edge], nodes: List[Node]) : List[Edge] = nodes match {
    case Nil => edges
    case h::tail => makeNonObservable(makeNonObservable(edges, h), tail)
  }

  def makeNonObservable(edges: List[Edge], node: Node) : List[Edge] = {
    // extract contingent edges on "node"
    val upper = edges.find(e => e.isInstanceOf[Upper] && e.asInstanceOf[Upper].lbl == node).get
    val lower = edges.find(e => e.isInstanceOf[Lower] && e.asInstanceOf[Lower].lbl == node).get

    val l = lower.d
    val u = upper.d
    val src = lower.from

    edges.filter(e => e != upper && e != lower).map {
      case Req(`node`,y,d, projs) => Req(src, y, l+d, projs ++ lower.proj)
      case Req(x,`node`,d, projs) => Req(x, src, d+u, projs ++ upper.proj)
      case Lower(`node`, y, d, lbl, projs) => Lower(src, y, l+d, lbl, projs ++ lower.proj)
      case Upper(x, `node`, d, lbl, projs) => Upper(x, src, u+d, lbl, projs ++ upper.proj)
      case e => assert(e.from != node && e.to != node); e
    }
  }

  def putInNormalForm(edges: List[Edge]) : List[Edge] = {
    var nextNode = edges.flatMap(e => List(e.from,e.to)).max +1
    def newNode() :Node = {nextNode+=1 ; nextNode-1}
    val addNodes = edges.collect { case l:Lower => (l.from, (newNode(), l.d)) }.toMap
    edges.flatMap {
      case Lower(from, to, d, lbl, projs) =>
        val (virt, dist) = addNodes(from)
        assert(dist == d)
        Lower(virt, to, 0, lbl, projs) ::
          Req(from, virt, d, Set()) :: Req(virt, from, -d, Set()) ::Nil
      case Upper(from, to, d, lbl, projs) =>
        Upper(addNodes(to)._1, from, d+addNodes(to)._2, lbl, projs) :: Nil
      case r:Req =>
        r :: Nil
    }
  }

  def getMinimalObservationSets(edges: List[Edge], observed: Set[Node]) : List[Set[Node]] = {
    val ctgs = edges.filter(_.isInstanceOf[Upper]).map(_.asInstanceOf[Upper].lbl).toSet
    val solutions = MSet[Set[Node]]()
    var queue = MSet[Set[Node]]()
    queue += Set()

    while(queue.nonEmpty) {
      val candidate = queue.head
      queue -= candidate

      val nonObs = ctgs -- candidate -- observed
      val allObsEdges = putInNormalForm(makePossiblyObservable(edges, nonObs.toList))

      val stnu = new DCMorris()
      for(e <- allObsEdges)
        stnu.addEdge(e)

      if(candidate.nonEmpty)
        println("new candidate: "+candidate)


      log(s"Candidate: $candidate")
      stnu.determineDC() match {
        case (true, None) =>
          log("  Valid")
          solutions.add(candidate)
        case (false, Some(possiblyObservable)) =>
          for(n <- possiblyObservable) {
            log(s"new: ${candidate+n}")
            queue += candidate + n
          }
      }
    }
    val minSols = solutions.filterNot(s => solutions.exists(s2 => s2 != s && s2.subsetOf(s))).toList
    log(s"Solutions: $minSols, all: $solutions")

    minSols
  }

  val ex1 = List(
    cont(A,B,0,5), reqs(B,C,20,25), reqs(B,D,20,25), cont(F,E,0,5), reqs(E,C,1,6), reqs(E,D,0,5)
  ).flatten
  val ex2 = List(
    cont(A,B,0,2), cont(B,C,0,2), reqs(C,D,0,2)
  ).flatten

//  log("\nObserve example:")
//  assertDC(ex1)

//  log("\nTwo non-obs:")
//  assertNotDC(makeNonObservable(ex1, List(B,E)))

//  log("\nOne non-obs:")
//  assertDC(makeNonObservable(ex1, List(E)))
//  assertNotDC(makeNonObservable(ex1, List(B)))
//
//  getMinimalObservationSets(ex1)

  getMinimalObservationSets(ex2, Set())

  def cont(src:Node, dst: Node, minDur:Int, maxDur:Int) = {
    List(Upper(dst, src, -maxDur, dst, Set()), Lower(src, dst, minDur, dst, Set()))
  }
  def reqs(src:Node, dst:Node, min:Int, max:Int) = {
    List(Req(src, dst, max, Set()), Req(dst, src, -min, Set()))
  }

  def get[ID](constraints: Seq[Constraint[ID]], observed: Set[TPRef], observable: Set[TPRef]) : Iterable[Set[TPRef]] = {
    implicit def toInt(tp: TPRef): Node = tp.id
    val tps = constraints.flatMap(c => List(c.u, c.v)).toSet
    val tpsFromInt = tps.map(tp => (tp.id, tp)).toMap
    val noVirtuals =
      constraints.map(c =>
        if(!c.u.isVirtual) c
        else new Constraint[ID](c.u.attachmentToReal._1, c.v, c.d+c.u.attachmentToReal._2, c.tipe, c.optID)
      ).map(c =>
        if(!c.v.isVirtual) c
        else new Constraint[ID](c.u, c.v.attachmentToReal._1, c.d -c.v.attachmentToReal._2, c.tipe, c.optID))

    val edges = noVirtuals.map(c =>
      if(c.tipe == ElemStatus.CONTINGENT && c.d <= 0)
        Lower(c.v, c.u, -c.d, c.u, Set())
      else if(c.tipe == ElemStatus.CONTINGENT)
        Upper(c.v, c.u, -c.d, c.v, Set())
      else
        Req(c.u, c.v, c.d, Set())
    ).toList

    val nonObservable = tps.filter(tp => tp.isContingent && !observable.contains(tp) && !observed.contains(tp)) //.map(toInt)

    val delNonObs = makeNonObservable(edges, nonObservable.map(toInt).toList)
//    val finalEdges = makePossiblyObservable(delNonObs, observable.map(toInt).toList)

    getMinimalObservationSets(delNonObs, observed.map(_.id))
      .map(sets => sets.map(i => tpsFromInt(i)))
  }

  def getResolvers(constraints: ju.List[Constraint[GlobalRef]], observed: ju.Set[TPRef], observable: ju.Set[TPRef]) : Optional[NeededObservations] = {
    val ret = get(constraints.asScala, observed.asScala.toSet, observable.asScala.toSet)
    ret match {
      case x :: Nil if x.isEmpty => Optional.empty()
      case resolvers => Optional.of(NeededObservations(resolvers.map(s => s.asJava).asJavaCollection))
    }
  }


}