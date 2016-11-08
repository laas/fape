package fr.laas.fape.constraints.stnu.structurals

import fr.laas.fape.anml.model.concrete.{ContingentConstraint, MinDelayConstraint, TPRef, TemporalConstraint}
import fr.laas.fape.constraints.stn.STN
import fr.laas.fape.constraints.stnu.InconsistentTemporalNetwork
import fr.laas.fape.constraints.stnu.parser.STNUParser
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter

import scala.collection.mutable

object StnWithStructurals {

  val INF: Int = Int.MaxValue /2 -1 // set to avoid overflow on addition of int values
  val NIL: Int = 0

  def buildFromString(str: String) : StnWithStructurals[String] = {
    val stn = new StnWithStructurals[String]()
    val parser = new STNUParser
    parser.parseAll(parser.problem, str) match {
      case parser.Success((tps,constraints, optStart, optEnd),_) => {
        for(tp <- tps) {
          stn.recordTimePoint(tp)
        }
        optStart match {
          case Some(start) => stn.setStart(start)
          case None =>
        }
        optEnd match {
          case Some(end) => stn.setEnd(end)
          case None =>
        }
        for(constraint <- constraints) {
          stn.addConstraint(constraint)
        }
      }
    }
    stn
  }

  class Edge(from: TPRef, to: TPRef, value: Int)
}

import StnWithStructurals._

class StnWithStructurals[ID](val nonRigidIndexes: mutable.Map[TPRef,Int],
                             val timepointByIndex: mutable.ArrayBuffer[TPRef],
                             var dist: DistanceMatrix,
                             val rigidRelations: RigidRelations,
                             val contingentLinks: mutable.ArrayBuffer[ContingentConstraint],
                             var optStart: Option[TPRef],
                             var optEnd: Option[TPRef],
                             var consistent: Boolean
                            )
  extends STN[TPRef,ID] with DistanceMatrixListener {

  def this() = this(mutable.Map(), mutable.ArrayBuffer(), new DistanceMatrix(), new RigidRelations(), mutable.ArrayBuffer(), None, None, true)

  dist.listeners += this

  def timepoints = nonRigidIndexes.keySet ++ rigidRelations._anchorOf.keySet

  private def toIndex(tp:TPRef) : Int = nonRigidIndexes(tp)

  private def isKnown(tp: TPRef) = nonRigidIndexes.contains(tp) || rigidRelations.isAnchored(tp)

  override def recordTimePoint(tp: TPRef): Int = {
    assert(!isKnown(tp))
    val id = dist.createNewNode()
    nonRigidIndexes.put(tp, id)
    while(timepointByIndex.size <= id) {
      timepointByIndex.append(null)
    }
    assert(timepointByIndex(id) == null)
    timepointByIndex(id) = tp
    optEnd match {
      case Some(end)  => enforceMinDelay(tp, end, 0)
      case None =>
    }
    id
  }

  def addMinDelay(from:TPRef, to:TPRef, minDelay:Int) =
    addEdge(to, from, -minDelay)

  def addMaxDelay(from: TPRef, to: TPRef, maxDelay: Int) =
    addMinDelay(to, from, -maxDelay)

  private def addEdge(a:TPRef, b :TPRef, t:Int): Unit = {
    if(!isKnown(a))
      recordTimePoint(a)
    if(!isKnown(b))
      recordTimePoint(b)

    val (aRef:TPRef, aToRef:Int) =
      if(rigidRelations.isAnchored(a))
        (rigidRelations._anchorOf(a), rigidRelations.distToAnchor(a))
      else
        (a, 0)
    val (bRef:TPRef, refToB) =
      if(rigidRelations.isAnchored(b))
        (rigidRelations._anchorOf(b), rigidRelations.distFromAnchor(b))
      else (b, 0)
    dist.enforceDist(toIndex(aRef), toIndex(bRef), aToRef + t + refToB)
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
    if(dist.getDistance(a,b) + dist.getDistance(b,a) < 0) {
      consistent = false
      throw new InconsistentTemporalNetwork
    }

    if(a == b)
      return

    val tpA = timepointByIndex(a) // TODO should go in if block to avoid unnecessary queries
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

  /** Makes an independent clone of this STN. */
  override def deepCopy(): STN[TPRef, ID] = ???

  /** Returns the maximal time from the start of the STN to u */
  override def getLatestStartTime(u: TPRef): Int = ???

  /** Record this time point as the global start of the STN */
  override def recordTimePointAsStart(tp: TPRef): Int = {
    if(!isKnown(tp))
      recordTimePoint(tp)
    setEnd(tp)
    nonRigidIndexes(tp)
  }

  def setStart(start: TPRef): Unit = {
    assert(isKnown(start))
    assert(optStart.isEmpty || optStart.get == start)
    optStart = Some(start)
    optEnd match {
      case Some(end) => enforceMinDelay(start, end, 0)
      case None =>
    }
  }

  /** Unifies this time point with the global end of the STN */
  override def recordTimePointAsEnd(tp: TPRef): Int = {
    if(!isKnown(tp))
      recordTimePoint(tp)
    setEnd(tp)
    nonRigidIndexes(tp)
  }

  def setEnd(end: TPRef): Unit = {
    assert(isKnown(end))
    assert(optEnd.isEmpty || optEnd.get == end)
    optEnd = Some(end)
    for(tp <- timepoints) {
      enforceBefore(tp, end)
    }
    optStart match {
      case Some(start) => enforceMinDelay(start, end, 0)
      case None =>
    }
  }

  /** Returns true if the STN is consistent (might trigger a propagation */
  override def isConsistent(): Boolean = consistent

  /** Removes all constraints that were recorded with this id */
  override def removeConstraintsWithID(id: ID): Boolean = ???

  override protected def addConstraint(u: TPRef, v: TPRef, w: Int): Unit =
    addMaxDelay(u, v, w)

  override protected def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean =
    w + rigidAwareDist(v, u) >= 0

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit = ???

  override def getEndTimePoint: Option[TPRef] = optEnd

  override def getStartTimePoint: Option[TPRef] = optStart

  /** Remove a timepoint and all associated constraints from the STN */
  override def removeTimePoint(tp: TPRef): Unit = ???

  /** Set the distance from the global start of the STN to tp to time */
  override def setTime(tp: TPRef, time: Int): Unit =
    optStart match {
      case Some(st) =>
        addMinDelay(st, tp, time)
        addMaxDelay(st, tp, time)
      case None => sys.error("This STN has no start timepoint")
    }


  /** Returns the minimal time from the start of the STN to u */
  override def getEarliestStartTime(u: TPRef): Int =
    optStart match {
      case Some(st) => minDelay(st, u)
      case None => sys.error("This STN has no start timepoint")
    }

  override protected def addConstraintWithID(u: TPRef, v: TPRef, w: Int, id: ID): Unit =
    addConstraint(u, v, w)
}
