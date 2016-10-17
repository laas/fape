package fr.laas.fape.constraints.stnu

import Controllability._
import fr.laas.fape.anml.model.concrete.TPRef
import fr.laas.fape.constraints.stn.CoreSTN
import planstack.structures.Converters._
import planstack.structures.IList
import ElemStatus._

case class Constraint[ID](u:TPRef, v:TPRef, d:Int, tipe:ElemStatus, optID:Option[ID]) {
  override def toString =
    (if(tipe==CONTINGENT) "cont:" else "") +
      "(%s -- %s --> %s ".format(u, d, v) +
      (if(optID.nonEmpty) "("+optID.get+")" else "")
}

abstract class GenSTNUManager[ID]
(
  var tps: Array[TPRef],
  var id : Array[Int], // -1 if virtual, -2 if not recorded, otherwise the ID in the underlying STN
  var rawConstraints : List[Constraint[ID]],
  var start : Option[TPRef],
  var end : Option[TPRef])
  extends STNU[ID]
{
  type Const = Constraint[ID]

  final def hasTimePoint(tp: TPRef) = id.length > tp.id && id(tp.id) != -2 //tps.containsKey(tp.id) || isVirtual(tp)
  final def isVirtual(tp: TPRef) = tp.isVirtual //tps.containsKey(tp.id) && tps.get(tp.id).isVirtual
  final def isPendingVirtual(tp: TPRef) = isVirtual(tp) && !tp.isAttached //tps.get(tp.id).refToReal.isEmpty
//  final def idInSTN(tp: TPRef) =  //tps.get(tp.id)
  final def add(timePoint: TPRef) = {
    assert(tps(timePoint.id) == null)
    assert(id(timePoint.id) != -2, "Make sure you updated the ID of this timepoint.")
    tps(timePoint.id) = timePoint
  }

  final def rm(tp: TPRef) = { id(tp.id) = -2 ; tps(tp.id) = null }//tps.remove(tp.id)
  final def refToReal(tp: TPRef) : (TPRef, Int) = { assert(tp.isAttached) ; tp.attachmentToReal }

  def stn : CoreSTN[ID]

  private final def ensureSpaceFor(tp: TPRef): Unit = {
    if(tps.size <= tp.id) {
      val newSize =Math.max(tp.id+1, tps.length*2)
      val tmp = tps
      tps = Array.fill(newSize)(null)
      Array.copy(tmp, 0, tps, 0, tmp.length)
      val tmp2 = id
      id = Array.fill(newSize)(-2)
      Array.copy(tmp2, 0, id, 0, tmp2.length)
    }
  }

  protected def commitContingent(u:Int, v:Int, d:Int, optID:Option[ID])

  final def enforceContingent(u:TPRef, v:TPRef, min:Int, max:Int, optID:Option[ID]) {
    assert(hasTimePoint(u) && !isVirtual(u), "Cannot add contingent constraint on virtual timepoints")
    assert(hasTimePoint(v) && !isVirtual(v), "Cannot add contingent constraint on virtual timepoints")
    val c1 = new Const(u, v, max, CONTINGENT, optID)
    val c2 = new Const(v, u, -min, CONTINGENT, optID)
    rawConstraints = c1 :: c2 :: rawConstraints
    commit(c1)
    commit(c2)
  }

  final def addDispatchableTimePoint(tp : TPRef) : Int = {
    tp.setDispatchable() //TODO: change to assert
    record(tp)
  }
  final def addContingentTimePoint(tp : TPRef) : Int = {
    tp.setContingent() //TODO: change to assert
    record(tp)
  }

  override final def recordTimePoint(tp: TPRef): Int = {
    tp.setStructural() //TODO: change to assert
    record(tp)
  }

  private def record(tp:TPRef): Int = {
    if(!hasTimePoint(tp)) {
      ensureSpaceFor(tp)
      id(tp.id) = stn.addVar()
      add(tp)
    }
    id(tp.id)
  }

  override final def removeTimePoint(tp: TPRef): Unit = {
    stn.removeVar(tp.id)
    rm(tp)
  }

  /** Removes all constraints that were recorded with this id */
  final override def removeConstraintsWithID(id: ID): Boolean = {
    rawConstraints = rawConstraints.filter(c => c.optID.isEmpty || c.optID.get != id)
    performRemoveConstraintWithID(id)
  }

  /** should remove a constraint from the underlying STNU */
  protected def performRemoveConstraintWithID(id: ID) : Boolean


  final override protected def addConstraintWithID(u: TPRef, v: TPRef, w: Int, cID: ID): Unit = {
    val c = new Const(u, v, w, CONTROLLABLE, Some(cID))
    rawConstraints = c :: rawConstraints
    if(!isPendingVirtual(u) && !isPendingVirtual(v))
      commit(c)
  }

  final override protected def addConstraint(u: TPRef, v: TPRef, w: Int): Unit = {
    val c = new Const(u, v, w, CONTROLLABLE, None)
    rawConstraints = c :: rawConstraints
    if(!isPendingVirtual(u) && !isPendingVirtual(v))
      commit(c)
  }

  protected def commitConstraint(u:Int, v:Int, w:Int, optID:Option[ID])

  /** creates a virtual time point virt with the constraint virt -- [dist,dist] --> real */
  override def addVirtualTimePoint(virt: TPRef, real: TPRef, dist: Int) {
    assert(hasTimePoint(real), "This virtual time points points to a non-recored TP. Maybe use pendingVirtual.")
    assert(!hasTimePoint(virt), "There is already a time point "+virt)
    ensureSpaceFor(virt)
    virt.setVirtual()
    virt.attachToReal(real, -dist)
    id(virt.id) = -1
    add(virt)
  }

  /** Records a virtual time point that is still partially defined.
    * All constraints on this time point will only be processed when defined with method*/
  override def addPendingVirtualTimePoint(virt: TPRef): Unit = {
    assert(!hasTimePoint(virt), "There is already a time point "+virt)
    ensureSpaceFor(virt)
    virt.setVirtual()
    id(virt.id) = -1
    add(virt)
  }

  /** Set a constraint virt -- [dist,dist] --> real. virt must have been already recorded as a pending virtual TP */
  override def setVirtualTimePoint(virt: TPRef, real: TPRef, dist: Int): Unit = {
    assert(hasTimePoint(real), "This virtual time points points to a non-recorded TP. Maybe use pendingVirtual.")
    assert(isPendingVirtual(virt), "This method is only applicable to pending virtual timepoints.")
    virt.attachToReal(real, -dist)

    for(c <- rawConstraints if c.u == virt || c.v == virt) {
      if(!isPendingVirtual(c.u) && !isPendingVirtual(c.v))
        commit(c)
    }
  }

  /** Record this time point as the global start of the STN */
  override final def recordTimePointAsStart(tp: TPRef): Int = {
    assert(start.isEmpty, "This STN already has a start timepoint recorded.")
    assert(!hasTimePoint(tp), "Timepoint is already recorded.")
    ensureSpaceFor(tp)
    tp.setStructural()
    id(tp.id) = stn.start
    add(tp)
    start = Some(tp)
    stn.start
  }

  /** Unifies this time point with the global end of the STN */
  override final def recordTimePointAsEnd(tp: TPRef): Int = {
    assert(end.isEmpty, "This STN already has a end timepoint recorded.")
    assert(!hasTimePoint(tp), "Timepoint is already recorded.")
    ensureSpaceFor(tp)
    tp.setStructural()
    id(tp.id) = stn.end
    add(tp)
    end = Some(tp)
    stn.end
  }

  override protected final def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean = {
    val (source, sourceDist) =
      if (isVirtual(u)) refToReal(u)
      else (u, 0)
    val (dest, destDist) =
      if (isVirtual(v)) refToReal(v)
      else (v, 0)

    assert(hasTimePoint(source) && !isVirtual(source))
    assert(hasTimePoint(dest) && !isVirtual(dest))

    isConstraintPossible(id(source.id), id(dest.id), sourceDist + w - destDist)
  }

  /** Is this constraint possible in the underlying stnu ? */
  protected def isConstraintPossible(u: Int, v: Int, w: Int): Boolean

  private final def commit(c : Const): Unit = {
    assert(!isPendingVirtual(c.u) && !isPendingVirtual(c.v), "One of the time points is a pending virtual")

    /** returns the real time point attached to a virtual time point and the distance between the two */
    def getSourceAndDist(source: TPRef, dist: Int) : (TPRef, Int) =
      if(!isVirtual(source)) (source, dist)
      else refToReal(source) match {
        case (s, d) => getSourceAndDist(s, d + dist)
      }

    val (source, sourceDist) = getSourceAndDist(c.u, 0)
    val (dest, destDist) = getSourceAndDist(c.v, 0)

    assert(hasTimePoint(source) && !isVirtual(source), "Time point not recorded: "+source)
    assert(hasTimePoint(dest) && !isVirtual(dest), "Time point not recorded: "+dest)

    if (c.tipe == CONTINGENT) {
      assert(!isVirtual(c.u) && !isVirtual(c.v), "Can't add a contingent constraints on virtual time points")
      commitContingent(id(source.id), id(dest.id), sourceDist + c.d - destDist, c.optID)
    } else {
      commitConstraint(id(source.id), id(dest.id), sourceDist + c.d - destDist, c.optID)
    }
  }

  final def checksPseudoControllability : Boolean = controllability.ordinal() >= PSEUDO_CONTROLLABILITY.ordinal()
  final def checksDynamicControllability : Boolean = controllability.ordinal() >= DYNAMIC_CONTROLLABILITY.ordinal()

  /** If there is a contingent constraint [min, max] between those two timepoints, it returns
    * Some((min, max).
    * Otherwise, None is returned.
    */
  def contingentDelay(from:TPRef, to:TPRef) : Option[(Integer, Integer)]

  def controllability : Controllability

  /** Makes an independent clone of this STN. */
  override def deepCopy(): GenSTNUManager[ID]

  /** Returns a list of all timepoints in this STNU, associated with a flag giving its status
    * (contingent or controllable. */
  final def timepoints : IList[TPRef] = tps.filter(_ != null).toList

  /** Returns the number of timepoints, excluding virtual time points */
  final def numRealTimePoints = id.size

  /** Returns the maximal time from the start of the STN to u */
  override final def getEarliestStartTime(u:TPRef) : Int = {
    assert(!isPendingVirtual(u), "Timepoint is virtual but has not been unified yet.")
    if(isVirtual(u)) {
      val (real, dist) = refToReal(u)
      getEarliestStartTime(real) + dist
    } else {
      earliestStart(id(u.id))
    }
  }

  /** Returns the maximal time from the start of the STN to u */
  override final def getLatestStartTime(u:TPRef) : Int = {
    assert(!isPendingVirtual(u), "Timepoint is virtual but has not been unified yet.")
    if(isVirtual(u)) {
      val (real, dist) = refToReal(u)
      getLatestStartTime(real) + dist
    } else {
      latestStart(id(u.id))
    }
  }

  /** Returns the earliest time for the time point with id u */
  protected def earliestStart(u:Int) : Int

  /** Returns the latest time for the time point with id u */
  protected def latestStart(u:Int) : Int

  def getMinDelay(u:TPRef, v:TPRef) : Int
  def getMaxDelay(u: TPRef, v:TPRef) : Int


  /** Returns a list of all constraints that were added to the STNU.
    * Each constraint is associated with flaw to distinguish between contingent and controllable ones. */
  final def constraints : IList[Const] = new IList[Const](rawConstraints)
}


