package fr.laas.fape.constraints.stnu.structurals

import fr.laas.fape.anml.model.concrete.{ContingentConstraint, MinDelayConstraint, TPRef, TemporalConstraint}
import fr.laas.fape.constraints.stnu.InconsistentTemporalNetwork

import scala.collection.mutable

object StnWithStructurals {

  val INF: Int = Int.MaxValue /2 -1 // set to avoid overflow on addition of int values
  val NIL: Int = 0
}

import StnWithStructurals._

class StnWithStructurals(val nonRigidIndexes: mutable.Map[TPRef,Int],
                         val timepointByIndex: mutable.ArrayBuffer[TPRef],
                         var dist: DistanceMatrix,
                         val rigidRelations: RigidRelations,
                         val contingentLinks: mutable.ArrayBuffer[ContingentConstraint]
                        )
  extends DistanceMatrixListener {


  private def toIndex(tp:TPRef) : Int = nonRigidIndexes(tp)

  private def isKnown(tp: TPRef) = nonRigidIndexes.contains(tp) || rigidRelations.isAnchored(tp)
  private def recordTimepoint(tp: TPRef): Int = {
    assert(!isKnown(tp))
    val id = dist.createNewNode()
    nonRigidIndexes.put(tp, id)
    if(timepointByIndex.size == id)
      timepointByIndex.append(tp)
    else {
      assert(timepointByIndex(id) == null)
      timepointByIndex(id) = tp
    }
    id
  }

  def addMinDelay(from:TPRef, to:TPRef, minDelay:Int) =
    addEdge(to, from, -minDelay)

  def addMaxDelay(from: TPRef, to: TPRef, maxDelay: Int) =
    addMinDelay(to, from, -maxDelay)

  private def addEdge(src:TPRef, dst :TPRef, t:Int): Unit = {
    if(!isKnown(src))
      recordTimepoint(src)
    if(!isKnown(dst))
      recordTimepoint(dst)
  }

  def addConstraint(c: TemporalConstraint): Unit = {
    c match {
      case req: MinDelayConstraint if req.minDelay.isKnown=>
        addMinDelay(req.src, req.dst, req.minDelay.get)
      case cont: ContingentConstraint if cont.min.isKnown && cont.max.isKnown =>
        addMinDelay(cont.src, cont.dst, cont.min.get)
        addMaxDelay(cont.src, cont.dst, cont.max.get)
        contingentLinks.append(cont)
      case _ =>
        throw new RuntimeException("Constraint: "+c+" is not properly supported")
    }
  }

  private def rigidAwareDist(a:TPRef, b:TPRef) : Int = {
    val (aRef:TPRef, aToRef:Int) =
      if(rigidRelations.isAnchored(a))
        (rigidRelations._anchorOf(a), rigidRelations.distToAnchor(a))
      else
        (a, 0)
    val (bRef:TPRef, refToB) =
      if(rigidRelations.isAnchored(b))
        (rigidRelations._anchorOf(b), rigidRelations.distFromAnchor(b))
      else (b, 0)

    aToRef + distanceBetweenNonRigid(aRef, bRef) + refToB
  }

  private def distanceBetweenNonRigid(a: TPRef, b: TPRef) = {
    dist.getDistance(toIndex(a), toIndex(b))
  }

  def concurrent(tp1: TPRef, tp2: TPRef) = rigidAwareDist(tp1,tp2) == rigidAwareDist(tp2,tp1)

  def minDelay(from: TPRef, to:TPRef) = -rigidAwareDist(to, from)
  def maxDelay(from: TPRef, to: TPRef) = rigidAwareDist(from, to)
  def beforeOrConcurrent(first: TPRef, second: TPRef) = rigidAwareDist(second, first) <= NIL
  def strictlyBefore(first: TPRef, second: TPRef) = rigidAwareDist(second, first) < NIL
  def between(tp: TPRef, min:TPRef, max:TPRef) = beforeOrConcurrent(min, tp) && beforeOrConcurrent(tp, max)
  def strictlyBetween(tp: TPRef, min:TPRef, max:TPRef) = strictlyBefore(min, tp) && strictlyBefore(tp, max)

  override def distanceUpdated(a: Int, b: Int): Unit = {
    // check if the network is now inconsistent
    if(dist.getDistance(a,b) + dist.getDistance(b,a) < 0)
      throw new InconsistentTemporalNetwork

    if(a == b)
      return

    val tpA = timepointByIndex(a)
    val tpB = timepointByIndex(b)
    assert(!rigidRelations.isAnchored(tpA))
    assert(!rigidRelations.isAnchored(tpB))

    // if there is a structural timepoint rigidly fixed to another, record this relation and simplify
    // the distance matrix
    if(dist.getDistance(a,b) == -dist.getDistance(b,a)) {
      if(tpA.genre.isStructural || tpB.genre.isStructural) {
        // record rigid relation
        rigidRelations.addRigidRelation(tpA, tpB, dist.getDistance(a, b))

        val (anchored, anchor) =
          if(rigidRelations.isAnchored(tpA)) (tpA, tpB)
          else if(rigidRelations.isAnchored(tpB)) (tpB,tpA)
          else throw new RuntimeException("No timepoint is considered as anchored after recording a new rigid relation")
        // remove the anchored timepoint from distance matrix
        dist.compileAwayRigid(toIndex(anchored), toIndex(anchor))
        timepointByIndex(toIndex(anchored)) = null
        nonRigidIndexes.remove(anchored)
      }
    }
  }
}
