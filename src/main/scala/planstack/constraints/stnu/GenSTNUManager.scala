package planstack.constraints.stnu

import net.openhft.koloboke.collect.map.hash.{HashIntObjMap, HashIntIntMap, HashIntIntMaps, HashIntObjMaps}
import net.openhft.koloboke.collect.map.{IntIntMap, IntObjMap}
import planstack.UniquelyIdentified
import planstack.anml.model.concrete.TPRef
import planstack.constraints.stn.{ISTN, GenSTNManager}
import ElemStatus._

import planstack.constraints.stnu.Controllability._
import planstack.structures.IList
import planstack.structures.Converters._

import scala.collection.JavaConversions

case class Constraint[ID](u:TPRef, v:TPRef, d:Int, tipe:ElemStatus, optID:Option[ID]) {
  override def toString =
    (if(tipe==CONTINGENT) "cont:" else "") +
      "(%s -- %s --> %s ".format(u, d, v) +
      (if(optID.nonEmpty) "("+optID.get+")" else "")
}

abstract class GenSTNUManager[ID]
(
  var tps: HashIntObjMap[TimePoint[TPRef]],
  var id : HashIntIntMap,
  var rawConstraints : List[Constraint[ID]],
  var start : Option[TPRef],
  var end : Option[TPRef])
  extends GenSTNManager[TPRef,ID]
{
  type Const = Constraint[ID]

  final def hasTimePoint(tp: TPRef) = tps.containsKey(tp.id) || isVirtual(tp)
  final def isVirtual(tp: TPRef) = tps.containsKey(tp.id) && tps.get(tp.id).isVirtual
  final def isPendingVirtual(tp: TPRef) = isVirtual(tp) && tps.get(tp.id).refToReal.isEmpty
  final def idInSTN(tp: TPRef) = tps.get(tp.id)
  final def add(timePoint: TimePoint[TPRef]) = tps.put(timePoint.tp.id, timePoint)
  final def rm(tp: TPRef) = tps.remove(tp.id)
  final def refToReal(tp: TPRef) : (TPRef, Int) = tps.get(tp.id).refToReal.get

  def stn : ISTN[ID]

  final def enforceContingent(u:TPRef, v:TPRef, min:Int, max:Int): Unit = {
    assert(hasTimePoint(u) && !isVirtual(u), "Cannot add contingent constraint on virtual timepoints")
    assert(hasTimePoint(v) && !isVirtual(v), "Cannot add contingent constraint on virtual timepoints")
    val c1 = new Const(u, v, max, CONTINGENT, None)
    val c2 = new Const(v, u, -min, CONTINGENT, None)
    rawConstraints = c1 :: c2 :: rawConstraints
    commit(c1)
    commit(c2)
  }

  protected def commitContingent(u:Int, v:Int, d:Int, optID:Option[ID])

  final def enforceContingentWithID(u:TPRef, v:TPRef, min:Int, max:Int, constID:ID) {
    assert(hasTimePoint(u) && !isVirtual(u), "Cannot add contingent constraint on virtual timepoints")
    assert(hasTimePoint(v) && !isVirtual(v), "Cannot add contingent constraint on virtual timepoints")
    val c1 = new Const(u, v, max, CONTINGENT, Some(constID))
    val c2 = new Const(v, u, -min, CONTINGENT, Some(constID))
    rawConstraints = c1 :: c2 :: rawConstraints
    commit(c1)
    commit(c2)
  }

  final def addControllableTimePoint(tp : TPRef) : Int = {
    assert(!hasTimePoint(tp), "Time point is already recorded: "+tp)
    assert(!id.containsKey(tp.id), "Time point is already recorded.")
    id.put(tp.id, stn.addVar())//id += ((tp.id, stn.addVar()))
    add(new DispatchableTimePoint[TPRef](tp))
    id.get(tp.id)
  }
  final def addContingentTimePoint(tp : TPRef) : Int = {
    assert(!hasTimePoint(tp), "Time point is already recorded: "+tp)
    assert(!id.containsKey(tp.id), "Time point is already recorded.")
    add(new ContingentTimePoint[TPRef](tp))
    id.put(tp.id, stn.addVar())
    id.get(tp.id)
  }

  override final def recordTimePoint(tp: TPRef): Int = addControllableTimePoint(tp)

  override final def removeTimePoint(tp: TPRef): Unit = {
    stn.removeVar(tp.id)
    id.remove(tp.id)
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

  /** Set the distance from the global start of the STN to tp to time */
  final override def setTime(tp: TPRef, time: Int): Unit = {
    assert(start.nonEmpty, "This stn has no recorded start time point.")
    addConstraint(start.get, tp, time)
    addConstraint(tp, start.get, -time)
  }

  protected def commitConstraint(u:Int, v:Int, w:Int, optID:Option[ID])

  /** creates a virtual time point virt with the constraint virt -- [dist,dist] --> real */
  def addVirtualTimePoint(virt: TPRef, real: TPRef, dist: Int) {
    assert(hasTimePoint(real), "This virtual time points points to a non-recored TP. Maybe use pendingVirtual.")
    assert(!hasTimePoint(virt), "There is already a time point "+virt)
    add(new VirtualTimePoint[TPRef](virt, Some(real, -dist)))
  }

  /** Records a virtual time point that is still partially defined.
    * All constraints on this time point will only be processed when defined with method*/
  def addPendingVirtualTimePoint(virt: TPRef): Unit = {
    assert(!hasTimePoint(virt), "There is already a time point "+virt)
    add(new VirtualTimePoint[TPRef](virt, None))
  }

  /** Set a constraint virt -- [dist,dist] --> real. virt must have been already recorded as a pending virtual TP */
  def setVirtualTimePoint(virt: TPRef, real: TPRef, dist: Int): Unit = {
    assert(hasTimePoint(real), "This virtual time points points to a non-recorded TP. Maybe use pendingVirtual.")
    assert(isPendingVirtual(virt), "This method is only applicable to pending virtual timepoints.")
    add(new VirtualTimePoint[TPRef](virt, Some(real, -dist)))

    for(c <- rawConstraints if c.u == virt || c.v == virt) {
      if(!isPendingVirtual(c.u) && !isPendingVirtual(c.v))
        commit(c)
    }
  }

  /** Record this time point as the global start of the STN */
  override final def recordTimePointAsStart(tp: TPRef): Int = {
    assert(start.isEmpty, "This STN already has a start timepoint recorded.")
    assert(!hasTimePoint(tp), "Timepoint is already recorded.")
    id.put(tp.id, stn.start)
    add(new StructuralTimePoint[TPRef](tp))
    start = Some(tp)
    stn.start
  }

  /** Unifies this time point with the global end of the STN */
  override final def recordTimePointAsEnd(tp: TPRef): Int = {
    assert(end.isEmpty, "This STN already has a end timepoint recorded.")
    assert(!hasTimePoint(tp), "Timepoint is already recorded.")
    id.put(tp.id, stn.end)
    add(new StructuralTimePoint[TPRef](tp))
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

    isConstraintPossible(id.get(source.id), id.get(dest.id), sourceDist + w - destDist)
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
      commitContingent(id.get(source.id), id.get(dest.id), sourceDist + c.d - destDist, c.optID)
    } else {
      commitConstraint(id.get(source.id), id.get(dest.id), sourceDist + c.d - destDist, c.optID)
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
  final def timepoints : IList[(TPRef, ElemStatus)] =
    for(tp <- JavaConversions.iterableAsScalaIterable(tps.values())) yield
      if(start.nonEmpty && tp.tp == start.get) (tp.tp, START)
      else if(end.nonEmpty && tp.tp == end.get) (tp.tp, END)
      else if(tp.isDispatchable) (tp.tp, CONTROLLABLE)
      else if(tp.isContingent) (tp.tp, CONTINGENT)
      else if(tp.isVirtual) (tp.tp, RIGID)
      else (tp.tp, NO_FLAG)

  /** Returns the number of timep oints, exclding virtual time points */
  final def numRealTimePoints = id.size


  final def getEndTimePoint: Option[TPRef] = end

  final def getStartTimePoint: Option[TPRef] = start

  /** Returns the maximal time from the start of the STN to u */
  override final def getEarliestStartTime(u:TPRef) : Int = {
    assert(!isPendingVirtual(u), "Timepoint is virtual but has not been unified yet.")
    if(isVirtual(u)) {
      val (real, dist) = refToReal(u)
      getEarliestStartTime(real) + dist
    } else {
      earliestStart(id.get(u.id))
    }
  }

  /** Returns the maximal time from the start of the STN to u */
  override final def getLatestStartTime(u:TPRef) : Int = {
    assert(!isPendingVirtual(u), "Timepoint is virtual but has not been unified yet.")
    if(isVirtual(u)) {
      val (real, dist) = refToReal(u)
      getLatestStartTime(real) + dist
    } else {
      latestStart(id.get(u.id))
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
  final def constraints : IList[Const] = new IList[Const]()
    rawConstraints ++
      (for(tp <- JavaConversions.iterableAsScalaIterable(tps.values()) if tp.isVirtual if tp.refToReal.nonEmpty) yield
        new Const(tp.refToReal.get._1, tp.tp, tp.refToReal.get._2, RIGID, None))
}


