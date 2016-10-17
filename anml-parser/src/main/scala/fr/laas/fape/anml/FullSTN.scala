package fr.laas.fape.anml

import fr.laas.fape.anml.model.AbstractParameterizedStateVariable
import fr.laas.fape.anml.pending.{GreaterEqualConstraint, IntExpression, LStateVariable, LesserEqualConstraint}
import fr.laas.fape.anml.pending.IntExpression._

import scala.collection.mutable

class FullSTN[AbsTP](timepointList: Seq[AbsTP]) {

  /**
    * A rigid set contains on reference timepoint.
    * All other time points are defined rigidly with respect
    * to this reference.
    * Any timepopint 'tp' in the set is defined with:
    *   time(reference) + dists(tp) = time(tp)
    */
  class RigidSet(var reference: Int) {
    val dists = mutable.Map[Int,Int]()
    dists(reference) = 0

    /** record a new rigid relation between those two timepoints.
      * At least one of them must be in the set already */
    def addRigidRelation(from: Int, to: Int, d: Int): Unit = {
      if(dists.contains(from))
        dists(to) = dists(from) + d
      else if(dists.contains(to))
        dists(from) = dists(to) -d
      else
        throw new RuntimeException("Neither timepoint was in the set.")

      rigids(from) = this
      rigids(to) = this
      assert(distFromRef(reference) == 0)
    }

    def merge(o: RigidSet, distBetweenRefs: Int): Unit = {
      for(t <- o.dists.keys)
        addRigidRelation(reference, t, distBetweenRefs + o.dists(t))

      assert(distFromRef(reference) == 0)
    }

    def contains(tp: Int) = dists.contains(tp)

    def members = dists.keys.toSet

    def distFromRef(tp: Int) = dists(tp)
  }

  case class AnchoredTimepointDefinition(timepoint: AbsTP, anchor: AbsTP, delay: Int)
  case class STNLikeConstraint(src: AbsTP, dst: AbsTP, label: IntExpression)

  type Result = List[(AbsTP,List[AbsTP])]

  implicit def toIndex(tp:AbsTP) : Int = tpIndexes(tp)

  val tpIndexes = timepointList.zipWithIndex.toMap
  val tps : Seq[AbsTP] = timepointList
  val size = tps.length

  val INF: IntExpression = IntExpression.lit((Int.MaxValue /2)-1) // set to avoid overflow on addition of int values
  val NIL: IntExpression = IntExpression.lit(0)

  val dist :Array[Array[IntExpression]] = (for (i <- 0 until size) yield Array.fill(size)(INF)).toArray[Array[IntExpression]]

  val references = mutable.Set[Int]()
  val rigids = mutable.Map[Int,RigidSet]()

  for(i <- 0 until size)
    dist(i)(i) = NIL

  def addMinDelay(from:AbsTP, to:AbsTP, minDelay:Int) =
    addEdge(to, from, IntExpression.lit(-minDelay))

  def addMinDelay(from:AbsTP, to:AbsTP, minDelay:IntExpression) =
    addEdge(to, from, minus(minDelay))

  def addEdge(src:AbsTP, dst :AbsTP, t:IntExpression): Unit = {
    dist(src)(dst) = min(dist(src)(dst), t)
  }



  private def rigidAwareDist(a:AbsTP, b:AbsTP) : IntExpression =
    rigidAwareDist(toIndex(a), toIndex(b))

  private def rigidAwareDist(a:Int, b:Int) : IntExpression = {
    val (aRef:Int, aRefToA:Int) =
      if(rigids.contains(a)) (rigids(a).reference, rigids(a).distFromRef(a))
      else (a, 0)
    val (bRef:Int, bRefToB) =
      if(rigids.contains(b)) (rigids(b).reference, rigids(b).distFromRef(b))
      else (b, 0)

    return sum(lit(bRefToB - aRefToA), dist(aRef)(bRef))
  }

  def concurrent(tp1: AbsTP, tp2: AbsTP) = rigidAwareDist(tp1,tp2) == rigidAwareDist(tp2,tp1)

  def minDelay(from: AbsTP, to:AbsTP) = minus(rigidAwareDist(to, from))
  def maxDelay(from: AbsTP, to: AbsTP) = rigidAwareDist(from, to)
  def beforeOrConcurrent(first: AbsTP, second: AbsTP) = lesserEqual(rigidAwareDist(second, first), NIL)
  def strictlyBefore(first: AbsTP, second: AbsTP) = lesserThan(rigidAwareDist(second, first), NIL)
  def between(tp: AbsTP, min:AbsTP, max:AbsTP) = beforeOrConcurrent(min, tp) && beforeOrConcurrent(tp, max)
  def strictlyBetween(tp: AbsTP, min:AbsTP, max:AbsTP) = strictlyBefore(min, tp) && strictlyBefore(tp, max)

  /** Check whether two timepoints are rigidly constrained.
    * If this is the case, the will be added to the relevant rigid set
    * and removed from the distance matrix. */
  private def checkRigid(t1: Int, t2: Int): Unit = {
    if(t1 == t2)
      return
    val d12 = dist(t1)(t2)
    val d21 = dist(t2)(t1)
    if(d12.ub == -d21.ub) {
      // we have a rigid relation
      if(rigids.contains(t1) && rigids.contains(t2)) {
        val rs1 = rigids(t1)
        val rs2 = rigids(t2)
        assert(rs1 != rs2, "Timepoints were already in the same rigid set")
        rs1.merge(rs2, d12.ub)
        references.remove(rs2.reference)
        enforceNewRigid(rs2.reference)
      } else if(!rigids.contains(t1) && !rigids.contains(t2)) {
        val rs = new RigidSet(t1)
        rs.addRigidRelation(t1, t2, d12.ub)
        references.add(t1)
        enforceNewRigid(t2)
      } else if(rigids.contains(t1)){
        val rs = rigids(t1)
        rs.addRigidRelation(t1,t2,d12.ub)
        enforceNewRigid(t2)
      } else {
        assert(rigids.contains(t2))
        val rs = rigids(t2)
        rs.addRigidRelation(t1,t2,d12.ub)
        enforceNewRigid(t1)
      }
    }
  }

  /** Returns false for any timepoint that was pruned out of the dist array because it was rigid */
  private def isActive(tp:Int) = !rigids.contains(tp) || references.contains(tp)

  /** Redirect all constraints involving a rigid time point to point
    * to its reference */
  def enforceNewRigid(tp: Int): Unit = {
    assert(!references.contains(tp))
    assert(rigids.contains(tp))
    val rs = rigids(tp)
    val ref = rs.reference
    val refToTp = rs.distFromRef(tp)
    for(i <- dist.indices if isActive(i)) {
      dist(ref)(i) = min(dist(ref)(i), sum(lit(refToTp), dist(tp)(i)))
      dist(i)(ref) = min(dist(i)(ref), sum(lit(-refToTp), dist(i)(tp)))
      dist(i)(tp) = null
      dist(tp)(i) = null // sabotage to make sure we get a null pointer exception we try to access those outdated fields
    }
  }

  /**
    * Makes a complete propagation using the floyd-warshall algorithm.
    * Online, this method: (i) infers bounds (lb and ub) on the variables
    * appearing on the distance matrix that must hold for the STN to be
    * consistent, (ii) detects "rigig" timpoint that have a fixed distance to a
    * reference timpoint. Those timepoints are compiled out of the distance matrix
    * to avoid useless propagation.
    */
  def floydWarshall(): Unit = {
    val svNodes = mutable.Map[AbstractParameterizedStateVariable, LStateVariable]()
    val lbs = mutable.Map[AbstractParameterizedStateVariable, Int]() //TODO: enforce those bounds
    val ubs = mutable.Map[AbstractParameterizedStateVariable, Int]()

    for(k <- 0 until size if isActive(k)) {
      for(i <- 0 until size if isActive(i) && isActive(k)) {
        for(j <- 0 until size if isActive(i) && isActive(j) && isActive(k)) {
          dist(i)(j) = min(dist(i)(j), sum(dist(i)(k), dist(k)(j)))
          val l = sum(dist(i)(j), dist(j)(i))
          assert(l.ub >= 0,  "Error: temporal inconsistency in the definition of this action")
          val inferredConstraints = geConstraint(l, 0)
          for(c <- inferredConstraints) {
            c match {
              case GreaterEqualConstraint(LStateVariable(sv, locLB, _), lb) =>
                lbs.update(sv, Math.max(lb, lbs.getOrElseUpdate(sv, locLB)))
              case LesserEqualConstraint(LStateVariable(sv, _, locUB), ub) =>
                ubs.update(sv, Math.min(ub, lbs.getOrElseUpdate(sv, locUB)))
              case _ =>
                // constraints is still a bit complex (e.g. involves two variables)
            }
          }
          val transfo = (e:IntExpression) => e match {
            case LStateVariable(sv, lb, ub) =>
              IntExpression.locSV(sv, lbs.getOrElse(sv, lb), ubs.getOrElse(sv, ub))
            case x =>
              x
          }

          dist(i)(j) = dist(i)(j).trans(transfo)
          dist(j)(i) = dist(j)(i).trans(transfo)
          checkRigid(i,j)
        }
      }
    }
//    println(lbs)
//    println(ubs)
  }

  /** Returns a list of (ref, rigids) where ref is a timepoint and rigids is a possibly emptu set
    * of that are rigidly attached to it. All timepoints of the STN are present either as a reference
    * or as a rigid timepoint. Timepoints in 'prio' are selected first to become references. */
  private def extractFlex(prio: List[AbsTP], pending:Set[AbsTP], result: Result) : Result = {
    prio match {
      case cur :: tail if !pending.contains(cur) =>
        // move to the next high priority item
        extractFlex(tail, pending, result)

      case l if pending.nonEmpty =>
        // select the next tp with high priority (or any tp if there is no
        // high priority item, and load all its rigid friends
        val (nextTP, tailPriority) =
          if(l.isEmpty) (pending.head, Nil)
          else (l.head, l.tail)

        if(rigids.contains(nextTP)) {
          val rs = rigids(nextTP)
          val related : Set[AbsTP] = rs.members.map(tps(_)) - nextTP
          extractFlex(tailPriority, (pending -- related) - nextTP, (nextTP, related.toList) :: result)
        } else {
          extractFlex(tailPriority, pending -nextTP, (nextTP, Nil) :: result)
        }

      case _ =>
        assert(pending.isEmpty)
        result
    }
  }

  /**
   * Builds a summary of this particular STN by identifying timepoints that are rigidly linked to others.
   * It returns a tuple consisting of:
   *   - a list of "flexible timepoints"
   *   - a list of anchored timepoints. Those timepoints have a fixed delay with respect to flexible timepoints
   *   - a list of constraints between flexible timepoints (APSP)
    *
    * @param priorityForFlexibleTimepoints Those time points will be the first to be considered to enter the set of flexible
   */
  def minimalRepresentation(priorityForFlexibleTimepoints: List[AbsTP]) : (List[AbsTP], List[STNLikeConstraint], List[AnchoredTimepointDefinition]) = {
    floydWarshall()
    val res = extractFlex(priorityForFlexibleTimepoints, tps.toSet, Nil)
    val flexibleTps = res.map(_._1)
    val rigidTps = res.foldLeft[List[AbsTP]](Nil)((allRigids, flexRigidPair) => allRigids ++ flexRigidPair._2)

    val delays : List[STNLikeConstraint] =
      for(flex1 <- flexibleTps ; flex2 <- flexibleTps ; if flex1 != flex2 && rigidAwareDist(flex1,flex2).lb < INF.get)
        yield STNLikeConstraint(flex1, flex2, rigidAwareDist(flex1,flex2))

    val rigids =
      for((flex, rigids) <- res ; rigid <- rigids)
        yield AnchoredTimepointDefinition(rigid, flex, rigidAwareDist(rigid,flex).get)

    (flexibleTps, delays, rigids)
  }
}
