package fr.laas.fape.constraints.stnu

import Controllability._
import fr.laas.fape.anml.model.concrete.{TPRef, TemporalConstraint}
import fr.laas.fape.constraints.stn.CoreSTN
import planstack.structures.Converters._
import planstack.structures.IList
import ElemStatus._
import fr.laas.fape.constraints.stnu.pseudo.PseudoSTNUManager

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
  final def add(timePoint: TPRef) = {
    assert(tps(timePoint.id) == null)
    assert(id(timePoint.id) != -2, "Make sure you updated the ID of this timepoint.")
    tps(timePoint.id) = timePoint
    if(end.nonEmpty) {
      enforceBefore(timePoint, end.get)
    }
  }

  final def rm(tp: TPRef) = { id(tp.id) = -2 ; tps(tp.id) = null }//tps.remove(tp.id)

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
    require(hasTimePoint(u))
    require(hasTimePoint(v))
    val c1 = new Const(u, v, max, CONTINGENT, optID)
    val c2 = new Const(v, u, -min, CONTINGENT, optID)
    rawConstraints = c1 :: c2 :: rawConstraints
    commit(c1)
    commit(c2)
  }

  override final def recordTimePoint(tp: TPRef): Int = {
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
    commit(c)
  }

  final override protected def addConstraint(u: TPRef, v: TPRef, w: Int): Unit = {
    val c = new Const(u, v, w, CONTROLLABLE, None)
    rawConstraints = c :: rawConstraints
    commit(c)
  }

  protected def commitConstraint(u:Int, v:Int, w:Int, optID:Option[ID])

  /** Record this time point as the global start of the STN */
  override final def recordTimePointAsStart(tp: TPRef): Int = {
    assert(start.isEmpty, "This STN already has a start timepoint recorded.")
    assert(!hasTimePoint(tp), "Timepoint is already recorded.")
    ensureSpaceFor(tp)
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
    id(tp.id) = stn.end
    add(tp)
    end = Some(tp)
    stn.end
  }

  override protected final def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean = {
    isConstraintPossible(id(u.id), id(v.id), w)
  }

  /** Is this constraint possible in the underlying stnu ? */
  protected def isConstraintPossible(u: Int, v: Int, w: Int): Boolean

  private final def commit(c : Const): Unit = {
    if (c.tipe == CONTINGENT) {
      commitContingent(id(c.u.id), id(c.v.id), c.d, c.optID)
    } else {
      commitConstraint(id(c.u.id), id(c.v.id), c.d, c.optID)
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

  /** Returns the maximal time from the start of the STN to u */
  override final def getEarliestTime(u:TPRef) : Int = {
    earliestStart(id(u.id))
  }

  /** Returns the maximal time from the start of the STN to u */
  override final def getLatestTime(u:TPRef) : Int = {
    latestStart(id(u.id))
  }

  /** Returns the earliest time for the time point with id u */
  protected def earliestStart(u:Int) : Int

  /** Returns the latest time for the time point with id u */
  protected def latestStart(u:Int) : Int

  def getMinDelay(u:TPRef, v:TPRef) : Int
  def getMaxDelay(u: TPRef, v:TPRef) : Int

  /** Returns a list of all constraints that were added to the STNU.
    * Each constraint is associated with flaw to distinguish between contingent and controllable ones. */
  override def constraints : IList[TemporalConstraint] = ???
}


