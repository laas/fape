package fr.laas.fape.constraints.stnu.morris

import java.util.Optional
import java.{util => ju}

import DCMorris._
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.constraints.stnu.structurals.StnWithStructurals

import scala.collection.JavaConverters._
import scala.collection.immutable.Set
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer => Buff, HashMap => MMap, PriorityQueue => Queue, Set => MSet}
import scala.language.implicitConversions

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

  def checkDynamicControllability(tn: TemporalNetwork) = {
    val morris = new DCMorris()
    for(e <- tn.constraints)
      morris.addEdge(e)
    morris.determineDC()
  }
}

class NotDC(val involvedProjections: Set[Proj]) extends Exception

class DCMorris {
  import DCMorris._
  val infty = 999999999

  /** All edges needed for DC checking (omiting edges needed for dispatchability only) */
  val edges = Buff[Edge]()
  val inEdges = MMap[Node, Buff[Edge]]()
  val outEdges = MMap[Node, Buff[Edge]]()

  /** all infered edges that are needed for dispatching but not for DC checking */
  val edgesForDispatchability = mutable.ArrayBuffer[Edge]()

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
  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[BackpropHist]): Unit = {
    val negReq = inEdges(source).filter(e => e.isInstanceOf[Req] && e.d < 0).map(_.asInstanceOf[Req]).toList
    dcBackprop(source, propagated, callHistory, Right(negReq))
    for(e <- inEdges(source) if e.d < 0 && e.isInstanceOf[Upper])
      dcBackprop(source, propagated, callHistory, Left(e.asInstanceOf[Upper]))
    propagated += source
  }

  /** Represents an edge "pivot -- dist --> source" implicitly computed by dcBackprop*/
  case class BackpropHist(pivot: Node, source: Node, dist: Int, projs: Set[Proj])
  @throws[NotDC]
  def dcBackprop(source: Node, propagated: Buff[Node], callHistory: List[BackpropHist], curEdges: Either[Upper, List[Req]]) : Unit = {
    case class QueueElem(n: Node, dist: Int, projs: Set[Proj])
    def suitable(e: Edge) = (curEdges, e) match  {
      case (Left(Upper(_, _, _, upLbl, _)) ,Lower(_, _, _, downLbl, _)) if upLbl == downLbl => false
      case _ => true
    }

    val visited = MSet[Node]()
    assert(!callHistory.exists(h => h.source == source), "This should have been caught earlier")

    if(propagated contains source)
      return ;
    val queue = Queue[QueueElem]()(Ordering.by(- _.dist))

    // edges to propagate from
    val toProp = curEdges match {
      case Left(up) =>
        assert(source == up.to)
        List(up)
      case Right(reqs) =>
        assert(reqs.forall(source == _.to))
        reqs
    }
    for(e <- toProp)
      queue += QueueElem(e.from, e.d, e.proj)

    while(queue.nonEmpty) {
      val QueueElem(cur, dist, projs) = queue.dequeue()
      if(!visited.contains(cur)) {
        visited += cur

        if(dist >= 0) {
          addEdge(new Req(cur, source, dist, projs))
        } else {
          // record infered edges that are needed for dispatchability but not for DC checking
          curEdges match {
            case Left(Upper(from, `source`, d, lbl, _)) =>
              edgesForDispatchability += Upper(cur, source, dist, lbl, projs)
            case Right(reqs) =>
              edgesForDispatchability += Req(cur, source, dist, projs)
    }
          if(isNegative(cur)) {
            if(source == cur) {  // negative loop
              throw new NotDC(projs)
            } else if (callHistory.exists(h => h.source == cur)) { // negative loop involving an edge previously computed by dcBackprop
              val previousProjs = callHistory.takeWhile(h => h.source == cur).flatMap(h => h.projs)
              throw new NotDC(projs ++ previousProjs)
            }
            dcBackprop(cur, propagated, BackpropHist(cur, source, dist, projs) :: callHistory)
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

  def makePossiblyObservable(edges: List[Edge], nodes: Set[Node]) : List[Edge] = {
    val lowers = mutable.Map[Node,Edge]()
    val uppers = mutable.Map[Node,Edge]()
    val processedNodes = mutable.Set[Node]()
    // edges that are final (not involved with any observable node)
    val finalEdges = mutable.Buffer[Edge]()
    // queues for each observable node, those edge will need to be compiled when the node is removed
    val edgesInvolved = mutable.Map[Node,mutable.Buffer[Edge]]()
    for(n <- nodes)
      edgesInvolved.put(n, mutable.Buffer[Edge]())

    // place the edge in the appropriate queue
    def sortEdge(e: Edge) {
      def isPending(n: Node) =
        nodes.contains(n) && !processedNodes.contains(n)
      e match {
        case x@Upper(from, to, d, lbl, _) if isPending(lbl) =>
          assert(from == lbl)
          uppers.put(lbl, x)
        case x@Lower(from, to, d, lbl, _) if isPending(lbl) =>
          assert(to == lbl)
          lowers.put(lbl, x)
        case _ =>
      }

      e match {
        // attach to the node with the smaller ID
        case x: Edge if isPending(x.from) && isPending(x.to) =>
          edgesInvolved(Math.min(x.from, x.to)) += x
        case x: Edge if isPending(x.from) =>
          edgesInvolved(x.from) += x
        case x: Edge if isPending(x.to) =>
          edgesInvolved(x.to) += x
        case x: Edge =>
          finalEdges += x
      }
    }

    // sort edges
    for(e <- edges)
      sortEdge(e)

    for(n <- nodes.toList.sorted) {
      processedNodes += n
      val upper = uppers(n)
      val lower = lowers(n)
      val l = lower.d
      val u = upper.d
      val src = lower.from

      // compile away all edges for this node
      for(e <- edgesInvolved(n) if e != upper && e != lower) {
        // create new edge
        val newEdge = e match {
          case Req(`n`,y,d, projs) => Req(src, y, l+d, projs ++ lower.proj + MinProj(n))
          case Req(x,`n`,d, projs) => Req(x, src, d+u, projs ++ upper.proj + MaxProj(n))
          case Lower(`n`, y, d, lbl, projs) => Lower(src, y, l+d, lbl, projs ++ lower.proj + MinProj(n))
          case Upper(x, `n`, d, lbl, projs) => Upper(x, src, u+d, lbl, projs ++ upper.proj + MaxProj(n))
          case x => assert(x.from != n && x.to != n); x
        }
        // place the edge in the appropriate queue
        sortEdge(newEdge)
      }
    }
    finalEdges.toList
  }
  /* Simpler but less efficient version of the above method
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
  }*/

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

    val newEdges = edges.filter(e => e != upper && e != lower).map {
      case Req(`node`,y,d, projs) => Req(src, y, l+d, projs ++ lower.proj)
      case Req(x,`node`,d, projs) => Req(x, src, d+u, projs ++ upper.proj)
      case Lower(`node`, y, d, lbl, projs) => Lower(src, y, l+d, lbl, projs ++ lower.proj)
      case Upper(x, `node`, d, lbl, projs) => Upper(x, src, u+d, lbl, projs ++ upper.proj)
      case e => assert(e.from != node && e.to != node); e
    }
    newEdges
  }

  var useLabelsForFocus : Boolean = true
  var allSolutions : Boolean = true
  var numIterations : Int = 0
  val debug = false

  def getMinimalObservationSets(tn: TemporalNetwork) : List[Set[Node]] = {
    assert(tn.invisibles.isEmpty)

    val solutions = MSet[Set[Node]]()
    val expanded = MSet[Set[Node]]() // to avoid duplicates
    var queue = MSet[Set[Node]]()
    queue += Set()

    // track the number of iterations to detect a timeout
    numIterations = 0

    while(queue.nonEmpty && numIterations < 5000) {
      val candidate = queue.head
      queue -= candidate
      expanded += candidate

      val alreadyKnowSimplerSolution =
        solutions.exists(sol => sol.forall(elem => candidate.contains(elem)))

      if (!alreadyKnowSimplerSolution) {
        numIterations += 1

        val stnu = tn
          .makeVisible(candidate)
          .supposeNonObservable
          .normalForm

        DCMorris.checkDynamicControllability(stnu) match {
          case (true, None) =>
            solutions.add(candidate)
            if (debug)
              println("  Solution!")
            if (!allSolutions)
              return solutions.toList
          case (false, Some(possiblyObservable)) =>
            val nextToConsider =
              if (useLabelsForFocus) possiblyObservable
              else tn.possiblyObservables -- candidate
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
    // retain only minimal solutions
    val minSols = solutions.filterNot(s => solutions.exists(s2 => s2 != s && s2.subsetOf(s))).toList

    minSols
  }

  def getResolvingObservationSets[ID](constraints: Seq[TemporalConstraint], observed: Set[TPRef], observable: Set[TPRef]) : Iterable[Set[TPRef]] = {
    val tnWithInvis = TemporalNetwork.build(constraints, observed, observable)
    val tn = tnWithInvis.withoutInvisible

    // build a map to find timepoint from their IDs
    val tps = constraints.flatMap(c => List(c.src, c.dst)).toSet
    val tpsFromInt = tps.map(tp => (tp.id, tp)).toMap

    getMinimalObservationSets(tn)
      .map(sets => sets.map(i => tpsFromInt(i)))
  }

  def getResolvers(constraints: ju.List[TemporalConstraint], observed: ju.Set[TPRef], observable: ju.Set[TPRef]) : Optional[NeededObservations] = {
    val ret = getResolvingObservationSets(constraints.asScala, observed.asScala.toSet, observable.asScala.toSet)
    ret match {
      case x :: Nil if x.isEmpty => Optional.empty()
      case resolvers => Optional.of(NeededObservations(resolvers.map(s => s.asJava).asJavaCollection))
    }
  }
}

/** Utils in order to benchmark the gains of inserting labels to extract needed observations */
object NeededObsBenchmarking extends App {
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

//  println(instrumentedGetMinimalObservationSets(ex1, Set()))
//  println(instrumentedGetMinimalObservationSets(ex2, Set()))




}