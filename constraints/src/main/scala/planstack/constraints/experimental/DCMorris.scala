package planstack.constraints.experimental

import java.util.Optional
import java.util.concurrent.TimeoutException

import planstack.anml.model.concrete.{GlobalRef, TPRef}
import planstack.constraints.experimental.DCMorris._
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
    val negReq = inEdges(source).filter(e => e.isInstanceOf[Req] && e.d < 0).map(_.asInstanceOf[Req]).toList
    dcBackprop(source, propagated, callHistory, Right(negReq))
    for(e <- inEdges(source) if e.d < 0 && e.isInstanceOf[Upper])
      dcBackprop(source, propagated, callHistory, Left(e.asInstanceOf[Upper]))
    propagated += source
  }

  @throws[NotDC]
  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[Node], curEdges: Either[Upper, List[Req]]) : Unit = {
    case class QueueElem(n: Node, dist: Int, projs: Set[Proj])
    def suitable(e: Edge) = (curEdges, e) match  {
      case (Left(Upper(_, _, _, upLbl, _)) ,Lower(_, _, _, downLbl, _)) if upLbl == downLbl => false
      case _ => true
    }

//    if(PartialObservability.debug)
//      println("    dcBackprop: "+source)

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
      if(!visited.contains(cur)) {
        visited += cur

//        if(PartialObservability.debug)
//          println("      pivot: "+cur+"  dist: "+dist)

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

object PartialObservability {
  import DCMorris._

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
    val addNodes = edges.collect { case l:Lower => (l.to, (newNode(), l.d)) }.toMap
    edges.flatMap {
      case Lower(from, to, d, lbl, projs) =>
        val (virt, dist) = addNodes(to)
        assert(dist == d, edges.mkString("\n"))
        Lower(virt, to, 0, lbl, projs) ::
          Req(from, virt, d, Set()) :: Req(virt, from, -d, Set()) ::Nil
      case Upper(from, to, d, lbl, projs) =>
        Upper(from, addNodes(from)._1, d+addNodes(from)._2, lbl, projs) :: Nil
      case r:Req =>
        r :: Nil
    }
  }

  var useLabelsForFocus : Boolean = true
  var allSolutions : Boolean = true
  var instrument : Boolean = false
  var numIterations : Int = 0
  var debug = false

  def getMinimalObservationSets(edges: List[Edge], observed: Set[Node]) : List[Set[Node]] = {
    val ctgs = edges.filter(_.isInstanceOf[Upper]).map(_.asInstanceOf[Upper].lbl).toSet
    val solutions = MSet[Set[Node]]()
    val expanded = MSet[Set[Node]]() // to avoid duplicates
    var queue = MSet[Set[Node]]()
    queue += Set()

    // track the number of iterations to detect a timeout
    numIterations = 0

    while(queue.nonEmpty) {
      val candidate = queue.head
      queue -= candidate
      expanded += candidate


      val alreadyKnowSimplerSolution =
        solutions.exists(sol => sol.forall(elem => candidate.contains(elem)))

      if (!alreadyKnowSimplerSolution) {
        numIterations += 1
        if (numIterations > 5000)
          return Nil // took to long lets say we have no solution

        val nonObs = ctgs -- candidate -- observed
        val allObsEdges = putInNormalForm(makePossiblyObservable(edges, nonObs.toList))

        if (debug) {
          println("  candidate: " + candidate)
        }



        val stnu = new DCMorris()
        for (e <- allObsEdges)
          stnu.addEdge(e)

        stnu.determineDC() match {
          case (true, None) =>
            solutions.add(candidate)
            if (debug)
              println("  Solution!")
            if (!allSolutions)
              return solutions.toList
          case (false, Some(possiblyObservable)) =>
            //          if(debug)
            //            println("  Lets keep searching")
            val nextToConsider =
              if (useLabelsForFocus) possiblyObservable
              else nonObs
            for (n <- nextToConsider) {
              val nextCandidate = candidate + n
              if (!expanded.contains(nextCandidate))
                queue += nextCandidate
            }
          case x =>
            throw new RuntimeException("Should be non reachable")
        }
      }
    }
    val minSols = solutions.filterNot(s => solutions.exists(s2 => s2 != s && s2.subsetOf(s))).toList

    minSols
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

//    NeededObsBenchmarking.instrumentedGetMinimalObservationSets(delNonObs, observed.map(_.id)) // can be used for benchmarking
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

/** Utils in order to benchmark the gains of inserting labels to extract needed observations */
object NeededObsBenchmarking extends App {
  import PartialObservability._
  val A = 1
  val B = 2
  val C = 3
  val D = 4
  val E = 5
  val F = 6

  def cont(src:Node, dst: Node, minDur:Int, maxDur:Int) = {
    List(Upper(dst, src, -maxDur, dst, Set()), Lower(src, dst, minDur, dst, Set()))
  }
  def reqs(src:Node, dst:Node, min:Int, max:Int) = {
    List(Req(src, dst, max, Set()), Req(dst, src, -min, Set()))
  }

  val ex1 = List(
    cont(A,B,0,5), reqs(B,C,20,25), reqs(B,D,20,25), cont(F,E,0,5), reqs(E,C,1,6), reqs(E,D,0,5)
  ).flatten

  val ex2 = List(
    cont(B,13,0,2), cont(B,12,0,2), cont(B,11,0,2), cont(B,10,0,2), cont(B,E,0,2), cont(A,F,0,2), cont(A,B,0,2), cont(B,C,0,2), reqs(C,D,0,2)
  ).flatten

  println(instrumentedGetMinimalObservationSets(ex1, Set()))
  println(instrumentedGetMinimalObservationSets(ex2, Set()))



  /** Drop in replacement for getMinimalObservationSet that will display some runtime information
    * on each invocation. */
  def instrumentedGetMinimalObservationSets(edges: List[Edge], observed: Set[Node]) : List[Set[Node]] = {
    val numContingents = edges.count(e => e.isInstanceOf[Upper])

    PartialObservability.instrument = true
    PartialObservability.useLabelsForFocus = true
    val ret = getMinimalObservationSets(edges, observed)
    val smartIter = PartialObservability.numIterations
    PartialObservability.useLabelsForFocus = false

    PartialObservability.debug = false
    val dumbIter = try {
      getMinimalObservationSets(edges, observed)
      PartialObservability.numIterations
    } catch {
      case ex:TimeoutException => 9999999
    }
    PartialObservability.useLabelsForFocus = true
    PartialObservability.instrument = false
    if(smartIter != 1 && dumbIter != 1)
      println(s"allsols ($smartIter, $dumbIter, $numContingents)")
    PartialObservability.debug = false

    val t1 = System.nanoTime()
    PartialObservability.allSolutions = false
    PartialObservability.instrument = true
    PartialObservability.useLabelsForFocus = true
    getMinimalObservationSets(edges, observed)
    val smartIterOne = PartialObservability.numIterations
    PartialObservability.useLabelsForFocus = false
    val t2 = System.nanoTime()

    val dumbIterOne = try {
      getMinimalObservationSets(edges, observed)
      PartialObservability.numIterations
    } catch {
      case ex:TimeoutException => 9999999
    }
    PartialObservability.useLabelsForFocus = true
    PartialObservability.instrument = false
    PartialObservability.allSolutions = true
    val t3 = System.nanoTime()
    if(smartIterOne != 1 && dumbIterOne != 1)
      println(s"onesol, $smartIterOne, $dumbIterOne, $numContingents, ${(t2-t1)/1000000f}, ${(t3-t2)/1000000f}")

    ret
  }
}