package planstack.constraints.stnu

import planstack.constraints.stnu.ElemStatus._
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter
import planstack.structures.IList
import planstack.structures.Converters._
import scala.language.implicitConversions

class STNUManager[TPRef,ID](val stnu : ISTNU[ID],
                            protected var id : Map[TPRef,Int],
                            protected var start : Option[TPRef],
                            protected var end : Option[TPRef])
  extends GenSTNUManager[TPRef,ID]
{
//  def this() = this(new FastIDC[ID](), Map(), None, None)
  def this() = this(new MMV[ID](), Map(), None, None)

  implicit def TPRef2stnID(tp : TPRef) : Int = id(tp)

  override def controllability = stnu.controllability

  override def enforceContingent(u: TPRef, v: TPRef, min: Int, max: Int): Unit = stnu.addContingent(u, v, min, max)

  override def enforceContingentWithID(u: TPRef, v: TPRef, min: Int, max: Int, id: ID): Unit = stnu.addContingentWithID(u, v, min, max, id)

  /** Makes an independent clone of this STN. */
  override def deepCopy(): STNUManager[TPRef, ID] = new STNUManager[TPRef,ID](stnu.cc(), id, start, end)

  /** Returns the maximal time from the start of the STN to u */
  override def getLatestStartTime(u: TPRef): Int = stnu.latestStart(u)

  /** Returns true if the STN is consistent (might trigger a propagation */
  override def isConsistent(): Boolean = stnu.consistent

  /** Removes all constraints that were recorded with this id */
  override def removeConstraintsWithID(id: ID): Boolean = stnu.removeConstraintsWithID(id)

  override protected def addConstraint(u: TPRef, v: TPRef, w: Int): Unit = stnu.addConstraint(u, v, w)

  override protected def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean = stnu.isConstraintPossible(u, v, w)

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit =
    stnu.writeToDotFile(filename)

  /** Records a new time point in the STN. Returns its ID (which might change) */
  override def recordTimePoint(tp: TPRef): Int = {
    assert(!id.contains(tp))
    id += ((tp, stnu.addVar()))
    id(tp)
  }


  override def addControllableTimePoint(tp: TPRef): Int =  {
    assert(!id.contains(tp))
    id += ((tp, stnu.addDispatchable()))
    id(tp)
  }

  override def addContingentTimePoint(tp: TPRef): Int = {
    assert(!id.contains(tp))
    id += ((tp, stnu.addContingentVar()))
    id(tp)
  }

  /** Set the distance from the global start of the STN to tp to time */
  override def setTime(tp: TPRef, time: Int): Unit = stnu.enforceInterval(stnu.start, tp, time, time)

  /** Returns the minimal time from the start of the STN to u */
  override def getEarliestStartTime(u: TPRef): Int = stnu.earliestStart(u)

  override protected def addConstraintWithID(u: TPRef, v: TPRef, w: Int, id: ID): Unit = stnu.addConstraintWithID(u, v, w, id)

  override def timepoints : IList[(TPRef, ElemStatus)] =
    for(tp <- id.keys) yield
      if(TPRef2stnID(tp) == stnu.start) (tp, START)
      else if(TPRef2stnID(tp) == stnu.end) (tp, END)
      else if(stnu.isDispatchable(tp)) (tp, CONTROLLABLE)
      else if(stnu.isContingent(tp)) (tp, CONTINGENT)
      else (tp, NO_FLAG)

  override def constraints : IList[(TPRef, TPRef, Int, ElemStatus, Option[ID])] = {
    val ref = id.map(_.swap)
    stnu.constraints.map((x:Tuple5[Int,Int,Int,ElemStatus, Option[ID]]) => (ref(x._1), ref(x._2), x._3, x._4, x._5))
  }

  override def removeTimePoint(tp:TPRef): Unit = {
    stnu.removeVar(id(tp))
    id = id - tp
  }

  /** Record this time point as the global start of the STN */
  override def recordTimePointAsStart(tp: TPRef): Int = {
    assert(start.isEmpty, "This STN already has a start timepoint recorded.")
    assert(!id.contains(tp), "Timepoint is already recorded.")
    id += ((tp, stnu.start))
    start = Some(tp)
    stnu.start
  }

  /** Unifies this time point with the global end of the STN */
  override def recordTimePointAsEnd(tp: TPRef): Int = {
    assert(end.isEmpty, "This STN already has a end timepoint recorded.")
    assert(!id.contains(tp), "Timepoint is already recorded.")
    id += ((tp, stnu.end))
    end = Some(tp)
    stnu.end
  }

  override def getEndTimePoint: Option[TPRef] = end

  override def getStartTimePoint: Option[TPRef] = start

  override def contingentDelay(from:TPRef, to:TPRef) = stnu.getContingentDelay(from, to) match {
    case Some((min, max)) => Some((min:Integer, max:Integer))
    case None => None
  }
}
