package planstack.constraints.stnu

import planstack.constraints.stn.ISTN
import planstack.constraints.stnu.ElemStatus.ElemStatus
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter
import Controllability._
import planstack.structures.IList
import ElemStatus._
import planstack.structures.Converters._

protected class TConstraint[TPRef,ID](val u:TPRef, val v:TPRef, val min:Int, val max:Int, val optID:Option[ID])

class PseudoSTNUManager[TPRef,ID](val stn : ISTN[ID],
                                  protected var contingents : List[TConstraint[TPRef,ID]],
                                  protected var id : Map[TPRef, Int],
                                  protected var controllableTPs : Set[TPRef],
                                  protected var contingentTPs : Set[TPRef],
                                  protected var start : Option[TPRef],
                                  protected var end : Option[TPRef])
  extends GenSTNUManager[TPRef,ID]
{
  def this() = this(new FullSTN[ID](), List(), Map(), Set(), Set(), None, None)
  def this(toCopy:PseudoSTNUManager[TPRef,ID]) = this(toCopy.stn.cc(), toCopy.contingents, toCopy.id,
    toCopy.controllableTPs, toCopy.contingentTPs, toCopy.start, toCopy.end)

  override def controllability = PSEUDO_CONTROLLABILITY

  override def enforceContingent(u: TPRef, v: TPRef, min: Int, max: Int): Unit = {
    contingents = new TConstraint(u, v, min, max, None.asInstanceOf[Option[ID]]) :: contingents
    stn.enforceInterval(id(u), id(v), min, max)
  }

  override def enforceContingentWithID(u: TPRef, v: TPRef, min: Int, max: Int, constID: ID): Unit = {
    contingents = new TConstraint(u, v, min, max, Some(constID)) :: contingents
    stn.enforceMaxDelayWithID(id(u), id(v), max, constID)
    stn.enforceMinDelayWithID(id(u), id(v), min, constID)
  }

  override def deepCopy(): PseudoSTNUManager[TPRef, ID] = new PseudoSTNUManager(this)

  override def getLatestStartTime(u: TPRef): Int = stn.latestStart(id(u))

  override def removeConstraintsWithID(constID: ID): Boolean = {
    stn.removeConstraintsWithID(constID)
    contingents = contingents.filter(c => c.optID match {
      case Some(oID) => oID != constID
      case None => true
    })
    isConsistent
  }

  override def isConsistent: Boolean = {
    stn.consistent && contingents.forall(c => stn.isMinDelayPossible(id(c.u), id(c.v), c.max) && stn.isMaxDelayPossible(id(c.u), id(c.v), c.min))
  }

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit =
    println("Warning: this STNUManager can not be exported to a dot file")

  override def recordTimePoint(tp: TPRef): Int = {
    assert(!id.contains(tp), "Time point is already recorded.")
    id += ((tp, stn.addVar()))
    id(tp)
  }

  override def addContingentTimePoint(tp : TPRef) : Int = {
    contingentTPs = contingentTPs + tp
    recordTimePoint(tp)
  }

  override def addControllableTimePoint(tp : TPRef) : Int = {
    controllableTPs = controllableTPs + tp
    recordTimePoint(tp)
  }

  override def getEarliestStartTime(u: TPRef): Int = stn.earliestStart(id(u))

  override protected def addConstraint(u: TPRef, v: TPRef, w: Int): Unit = stn.addConstraint(id(u), id(v), w)

  override protected def addConstraintWithID(u: TPRef, v: TPRef, w: Int, constID: ID): Unit = stn.addConstraintWithID(id(u), id(v), w, constID)

  override protected def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean = stn.isConstraintPossible(id(u), id(v), w)

  override def setTime(tp:TPRef, time:Int) { stn.enforceInterval(stn.start, id(tp), time, time) }

  override def timepoints : IList[(TPRef, ElemStatus)] =
    for(tp <- id.keys) yield
      if(id(tp) == stn.start) (tp, START)
      else if(id(tp) == stn.end) (tp, END)
      else if(controllableTPs.contains(tp)) (tp, CONTROLLABLE)
      else if(contingentTPs.contains(tp)) (tp, CONTINGENT)
      else (tp, NO_FLAG)

  override def constraints : IList[(TPRef, TPRef, Int, ElemStatus, Option[ID])] = {
    val ref = id.map(_.swap)
    var ret = new IList[(TPRef, TPRef, Int, ElemStatus, Option[ID])](
      stn.constraints.map((x:Tuple5[Int,Int,Int,ElemStatus,Option[ID]]) => (ref(x._1), ref(x._2), x._3, x._4, x._5)))
    for(cont <- contingents) {
      ret = ret.`with`(cont.u, cont.v, cont.max, CONTINGENT, cont.optID)
      ret = ret.`with`(cont.v, cont.u, -cont.min, CONTINGENT, cont.optID)
    }
    ret
  }

  override def removeTimePoint(tp:TPRef): Unit = {
    stn.removeVar(id(tp))
    id = id - tp
  }


  /** Record this time point as the global start of the STN */
  override def recordTimePointAsStart(tp: TPRef): Int = {
    assert(start.isEmpty, "This STN already has a start timepoint recorded.")
    assert(!id.contains(tp), "Timepoint is already recorded.")
    id += ((tp, stn.start))
    start = Some(tp)
    stn.start
  }

  /** Unifies this time point with the global end of the STN */
  override def recordTimePointAsEnd(tp: TPRef): Int = {
    assert(end.isEmpty, "This STN already has a end timepoint recorded.")
    assert(!id.contains(tp), "Timepoint is already recorded.")
    id += ((tp, stn.end))
    end = Some(tp)
    stn.end
  }

  override def getEndTimePoint: Option[TPRef] = end

  override def getStartTimePoint: Option[TPRef] = start
}
