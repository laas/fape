package planstack.constraints.stnu

import planstack.constraints.stn.GenSTNManager
import ElemStatus._

import planstack.constraints.stnu.Controllability._
import planstack.structures.IList
import planstack.structures.Converters._

case class Constraint[TPRef,ID](u:TPRef, v:TPRef, d:Int, tipe:ElemStatus, optID:Option[ID]) {
  override def toString =
    (if(tipe==CONTINGENT) "cont:" else "") +
      "(%s -- %s --> %s ".format(u, d, v) +
      (if(optID.nonEmpty) "("+optID.get+")" else "")
}

abstract class GenSTNUManager[TPRef,ID](var virtualTPs: Map[TPRef, Option[(TPRef, Int)]],
                                        var id : Map[TPRef, Int],
                                        var dispatchableTPs : Set[TPRef],
                                        var contingentTPs : Set[TPRef],
                                        var rawConstraints : List[Constraint[TPRef,ID]],
                                        var start : Option[TPRef],
                                        var end : Option[TPRef])
  extends GenSTNManager[TPRef,ID]
{
  type Const = Constraint[TPRef,ID]

  final def hasTimePoint(tp: TPRef) = id.contains(tp) || virtualTPs.contains(tp)
  final def isVirtual(tp: TPRef) = virtualTPs.contains(tp)
  final def isPendingVirtual(tp: TPRef) = virtualTPs.contains(tp) && virtualTPs(tp).isEmpty

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
    dispatchableTPs += tp
    recordTimePoint(tp)
  }
  final def addContingentTimePoint(tp : TPRef) : Int = {
    assert(!hasTimePoint(tp), "Time point is already recorded: "+tp)
    contingentTPs += tp
    recordTimePoint(tp)
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
    virtualTPs = virtualTPs.updated(virt, Some((real, -dist)))
  }

  /** Records a virtual time point that is still partially defined.
    * All constraints on this time point will only be processed when defined with method*/
  def addPendingVirtualTimePoint(virt: TPRef): Unit = {
    assert(!hasTimePoint(virt), "There is already a time point "+virt)
    virtualTPs = virtualTPs.updated(virt, None)
  }

  /** Set a constraint virt -- [dist,dist] --> real. virt must have been already recorded as a pending virtual TP */
  def setVirtualTimePoint(virt: TPRef, real: TPRef, dist: Int): Unit = {
    assert(hasTimePoint(real), "This virtual time points points to a non-recorded TP. Maybe use pendingVirtual.")
    assert(isPendingVirtual(virt), "This method is only applicable to pending virtual timepoints.")
    virtualTPs = virtualTPs.updated(virt, Some((real, -dist)))
    for(c <- rawConstraints if c.u == virt || c.v == virt) {
      if(!isPendingVirtual(c.u) && !isPendingVirtual(c.v))
        commit(c)
    }
  }

  override protected final def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean = {
    val (source, sourceDist) =
      if (isVirtual(u)) virtualTPs(u).get
      else (u, 0)
    val (dest, destDist) =
      if (isVirtual(v)) virtualTPs(v).get
      else (v, 0)

    assert(hasTimePoint(source) && !isVirtual(source))
    assert(hasTimePoint(dest) && !isVirtual(dest))

    isConstraintPossible(id(source), id(dest), sourceDist + w - destDist)
  }

  /** Is this constraint possible in the underlying stnu ? */
  protected def isConstraintPossible(u: Int, v: Int, w: Int): Boolean

  private final def commit(c : Const): Unit = {
    assert(!isPendingVirtual(c.u) && !isPendingVirtual(c.v), "One of the time points is a pending virtual")

    val (source, sourceDist) =
      if (isVirtual(c.u)) virtualTPs(c.u).get
      else (c.u, 0)
    val (dest, destDist) =
      if (isVirtual(c.v)) virtualTPs(c.v).get
      else (c.v, 0)

    assert(hasTimePoint(source) && !isVirtual(source))
    assert(hasTimePoint(dest) && !isVirtual(dest))

    if (c.tipe == CONTINGENT) {
      assert(!isVirtual(c.u) && !isVirtual(c.v), "Can't add a contingent constraints on virtual time points")
      commitContingent(id(source), id(dest), sourceDist + c.d - destDist, c.optID)
    } else {
      commitConstraint(id(source), id(dest), sourceDist + c.d - destDist, c.optID)
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
  override def deepCopy(): GenSTNUManager[TPRef, ID]

  /** Returns a list of all timepoints in this STNU, associated with a flag giving its status
    * (contingent or controllable. */
  final def timepoints : IList[(TPRef, ElemStatus)] =
    (for (tp <- id.keys) yield
      if(start.nonEmpty && tp == start.get) (tp, START)
      else if(end.nonEmpty && tp == end.get) (tp, END)
      else if (dispatchableTPs.contains(tp)) (tp, CONTROLLABLE)
      else if (contingentTPs.contains(tp)) (tp, CONTINGENT)
      else (tp, NO_FLAG)
    ) ++
      (for(tp <- virtualTPs.keys) yield (tp, RIGID))


  final def getEndTimePoint: Option[TPRef] = end

  final def getStartTimePoint: Option[TPRef] = start

  /** Returns the maximal time from the start of the STN to u */
  override final def getEarliestStartTime(u:TPRef) : Int = {
    assert(!isPendingVirtual(u), "Timepoint is virtual but has not been unified yet.")
    if(isVirtual(u)) {
      val (real, dist) = virtualTPs(u).get
      getEarliestStartTime(real) + dist
    } else {
      earliestStart(id(u))
    }
  }

  /** Returns the maximal time from the start of the STN to u */
  override final def getLatestStartTime(u:TPRef) : Int = {
    assert(!isPendingVirtual(u), "Timepoint is virtual but has not been unified yet.")
    if(isVirtual(u)) {
      val (real, dist) = virtualTPs(u).get
      getLatestStartTime(real) + dist
    } else {
      latestStart(id(u))
    }
  }

  /** Returns the earliest time for the time point with id u */
  protected def earliestStart(u:Int) : Int

  /** Returns the latest time for the time point with id u */
  protected def latestStart(u:Int) : Int


  /** Returns a list of all constraints that were added to the STNU.
    * Each constraint is associated with flaw to distinguish between contingent and controllable ones. */
  final def constraints : IList[Const] =
    rawConstraints ++
      (for((virt, definition) <- virtualTPs if definition.nonEmpty) yield
        new Const(definition.get._1, virt, definition.get._2, RIGID, None))
}


