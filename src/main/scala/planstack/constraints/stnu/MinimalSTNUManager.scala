package planstack.constraints.stnu

import planstack.constraints.stn.{STNIncBellmanFord, ISTN}
import planstack.constraints.stnu.ElemStatus._
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter
import planstack.structures.IList
import planstack.structures.Converters._
import scala.language.implicitConversions

class MinimalSTNUManager[TPRef,ID](val stn:ISTN[ID],
                                   var dispatchableVars : Set[TPRef],
                                   var contingentVars : Set[TPRef],
                                   var ids : Map[TPRef,Int],
                                   var contingents : List[(TPRef,TPRef,Int,Option[ID])],
                                   protected var start : Option[TPRef],
                                   protected var end : Option[TPRef])
  extends GenSTNUManager[TPRef,ID]
{
  implicit def ref2id(tp: TPRef) = ids(tp)

  def this() = this(new STNIncBellmanFord[ID](), Set(), Set(), Map(), List(), None, None)

  /** Makes an independent clone of this STN. */
  override def deepCopy(): MinimalSTNUManager[TPRef, ID] =
    new MinimalSTNUManager(stn.cc(), dispatchableVars, contingentVars, ids, contingents, start, end)

  override def timepoints: IList[(TPRef, ElemStatus)] =
    for (tp <- ids.keys) yield
      if(ref2id(tp) == stn.start) (tp, START)
      else if(ref2id(tp) == stn.end) (tp, END)
      else if (dispatchableVars.contains(tp)) (tp, CONTROLLABLE)
      else if (contingentVars.contains(tp)) (tp, CONTINGENT)
      else (tp, NO_FLAG)

  override def constraints: IList[(TPRef, TPRef, Int, ElemStatus, Option[ID])] = {
    val ref = ids.map(_.swap)
    val reqs = stn.constraints.map((x: Tuple5[Int, Int, Int, ElemStatus, Option[ID]]) => (ref(x._1), ref(x._2), x._3, x._4, x._5))
    val conts = contingents.map(x => (x._1, x._2, x._3, CONTINGENT, x._4))
    reqs ++ conts
  }

  override def removeTimePoint(tp: TPRef): Unit = {
    stn.removeVar(tp)
    ids = ids - tp
  }

  override def addControllableTimePoint(tp: TPRef): Int = {
    dispatchableVars = dispatchableVars + tp
    recordTimePoint(tp)
  }

  override def controllability: Controllability = Controllability.STN_CONSISTENCY

  override def enforceContingent(u: TPRef, v: TPRef, min: Int, max: Int): Unit = {
    stn.enforceInterval(u, v, min, max)
    contingents = (u, v, max, None) ::(v, u, -min, None) :: contingents
  }

  override def enforceContingentWithID(u: TPRef, v: TPRef, min: Int, max: Int, id: ID): Unit = {
    stn.enforceMinDelayWithID(u, v, min, id)
    stn.enforceMaxDelayWithID(u, v, max, id)
    contingents = (u, v, max, Some(id)) ::(v, u, -min, Some(id)) :: contingents
  }

  override def addContingentTimePoint(tp: TPRef): Int = {
    contingentVars = contingentVars + tp
    recordTimePoint(tp)
  }

  /** Returns the maximal time from the start of the STN to u */
  override def getLatestStartTime(u: TPRef): Int = stn.latestStart(u)

  /** Removes all constraints that were recorded with this id */
  override def removeConstraintsWithID(id: ID): Boolean = {
    contingents = contingents.filter(x => x._4.isEmpty || x._4.get != id)
    stn.removeConstraintsWithID(id)
}

  /** Returns true if the STN is consistent (might trigger a propagation */
  override def isConsistent: Boolean = stn.consistent

  override protected def addConstraint(u: TPRef, v: TPRef, w: Int): Unit =
    stn.addConstraint(u, v, w)

  override protected def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean =
    stn.isConstraintPossible(u, v, w)

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit =
  stn.writeToDotFile(filename)

  /** Records a new time point in the STN. Returns its ID (which might change) */
  override def recordTimePoint(tp: TPRef): Int = {
    assert(!ids.contains(tp))
    val id = stn.addVar()
    ids += ((tp, id))
    id
  }

  /** Set the distance from the global start of the STN to tp to time */
  override def setTime(tp: TPRef, time: Int): Unit =
    stn.enforceInterval(stn.start, tp, time, time)

  /** Returns the minimal time from the start of the STN to u */
  override def getEarliestStartTime(u: TPRef): Int =
    stn.earliestStart(u)

  override protected def addConstraintWithID(u: TPRef, v: TPRef, w: Int, id: ID): Unit =
    stn.addConstraintWithID(u, v, w, id)

  /** Record this time point as the global start of the STN */
  override def recordTimePointAsStart(tp: TPRef): Int = {
    assert(start.isEmpty, "A start timepoint was already recorded.")
    assert(!ids.contains(tp), "Timepoint "+tp+" is already recorded.")
    ids += ((tp, stn.start))
    start = Some(tp)
    stn.start
  }

  /** Unifies this time point with the global end of the STN */
  override def recordTimePointAsEnd(tp: TPRef): Int = {
    assert(end.isEmpty, "A end timepoint was already recorded.")
    assert(!ids.contains(tp), "Timepoint "+tp+" is already recorded.")
    ids += ((tp, stn.end))
    end = Some(tp)
    stn.end
  }

  override def getEndTimePoint: Option[TPRef] = end

  override def getStartTimePoint: Option[TPRef] = start
}
