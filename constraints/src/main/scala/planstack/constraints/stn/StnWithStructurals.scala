package planstack.constraints.stn

import scala.collection.mutable
import Math._

import planstack.anml.model.concrete.{ContingentConstraint, TPRef}
import planstack.constraints.stnu.InconsistentTemporalNetwork

object StnWithStructurals {

  val INF: Int = Int.MaxValue /2 -1 // set to avoid overflow on addition of int values
  val NIL: Int = 0
}

import StnWithStructurals._

class StnWithStructurals(val nonRigidIndexes: mutable.Map[TPRef,Int],
                         var dist: Array[Array[Int]],
                         val rigidRelations: RigidRelations,
                         val contingentLinks: mutable.Seq[ContingentConstraint]
                        ) {


  implicit def toIndex(tp:TPRef) : Int = tpIndexes(tp)

  def isKnown(tp: TPRef) = nonRigidIndexes.contains(tp) || rigidRelations.isAnchored(tp)



  def addMinDelay(from:TPRef, to:TPRef, minDelay:Int) =
    addEdge(to, from, -minDelay)

  def addEdge(src:TPRef, dst :TPRef, t:Int): Unit = {
    dist(src)(dst) = Math.min(dist(src)(dst), t)
  }



  private def rigidAwareDist(a:TPRef, b:TPRef) : Int =
    rigidAwareDist(toIndex(a), toIndex(b))

  private def rigidAwareDist(a:Int, b:Int) : Int = {
    val (aRef:Int, aRefToA:Int) =
      if(rigids.contains(a)) (rigids(a).reference, rigids(a).distFromRef(a))
      else (a, 0)
    val (bRef:Int, bRefToB) =
      if(rigids.contains(b)) (rigids(b).reference, rigids(b).distFromRef(b))
      else (b, 0)

    return bRefToB - aRefToA + dist(aRef)(bRef)
  }

  def concurrent(tp1: TPRef, tp2: TPRef) = rigidAwareDist(tp1,tp2) == rigidAwareDist(tp2,tp1)

  def minDelay(from: TPRef, to:TPRef) = -rigidAwareDist(to, from)
  def maxDelay(from: TPRef, to: TPRef) = rigidAwareDist(from, to)
  def beforeOrConcurrent(first: TPRef, second: TPRef) = rigidAwareDist(second, first) <= NIL
  def strictlyBefore(first: TPRef, second: TPRef) = rigidAwareDist(second, first) < NIL
  def between(tp: TPRef, min:TPRef, max:TPRef) = beforeOrConcurrent(min, tp) && beforeOrConcurrent(tp, max)
  def strictlyBetween(tp: TPRef, min:TPRef, max:TPRef) = strictlyBefore(min, tp) && strictlyBefore(tp, max)

  /** Check whether two timepoints are rigidly constrained.
    * If this is the case, the will be added to the relevant rigid set
    * and removed from the distance matrix. */
  private def checkRigid(t1: Int, t2: Int): Unit = {
    if(t1 == t2)
      return
//    val d12 = dist(t1)(t2)
//    val d21 = dist(t2)(t1)
//    if(d12 == -d21) {
//      // we have a rigid relation
//      if(rigids.contains(t1) && rigids.contains(t2)) {
//        val rs1 = rigids(t1)
//        val rs2 = rigids(t2)
//        assert(rs1 != rs2, "Timepoints were already in the same rigid set")
//        rs1.merge(rs2, d12)
//        references.remove(rs2.reference)
//        enforceNewRigid(rs2.reference)
//      } else if(!rigids.contains(t1) && !rigids.contains(t2)) {
//        val rs = new RigidSet(t1)
//        rs.addRigidRelation(t1, t2, d12)
//        references.add(t1)
//        enforceNewRigid(t2)
//      } else if(rigids.contains(t1)){
//        val rs = rigids(t1)
//        rs.addRigidRelation(t1,t2,d12)
//        enforceNewRigid(t2)
//      } else {
//        assert(rigids.contains(t2))
//        val rs = rigids(t2)
//        rs.addRigidRelation(t1,t2,d12)
//        enforceNewRigid(t1)
//      }
//    }
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
      dist(ref)(i) = Math.min(dist(ref)(i), refToTp + dist(tp)(i))
      dist(i)(ref) = Math.min(dist(i)(ref), -refToTp + dist(i)(tp))
//      dist(i)(tp) = null TODO
//      dist(tp)(i) = null // sabotage to make sure we get a null pointer exception we try to access those outdated fields
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
    for(k <- 0 until size if isActive(k)) {
      for(i <- 0 until size if isActive(i) && isActive(k)) {
        for(j <- 0 until size if isActive(i) && isActive(j) && isActive(k)) {
          if(dist(i)(j) > dist(i)(k) + dist(k)(j)) {
            dist(i)(j) = dist(i)(k) + dist(k)(j)
            if(dist(i)(j) + dist(j)(i) < 0)
              throw new InconsistentTemporalNetwork()

            checkRigid(i, j)
          }
        }
      }
    }
    //    println(lbs)
    //    println(ubs)
  }
}
