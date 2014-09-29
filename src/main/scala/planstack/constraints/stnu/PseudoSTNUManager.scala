package planstack.constraints.stnu

import planstack.constraints.stn.ISTN
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter

protected class TConstraint[TPRef,ID](val u:TPRef, val v:TPRef, val min:Int, val max:Int, val optID:Option[ID])

class PseudoSTNUManager[TPRef,ID](val stn : ISTN[ID],
                                  protected var contingents : List[TConstraint[TPRef,ID]],
                                  protected var id : Map[TPRef, Int])
  extends GenSTNUManager[TPRef,ID]
{
  def this() = this(new FullSTN[ID](), List(), Map())
  def this(toCopy:PseudoSTNUManager[TPRef,ID]) = this(toCopy.stn.cc(), toCopy.contingents, toCopy.id)

  override def checksPseudoConsistency = true

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

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit = ???

  override def recordTimePoint(tp: TPRef): Int = {
    assert(!id.contains(tp), "Time point is already recorded.")
    id += ((tp, stn.addVar()))
    id(tp)
  }

  override def getEarliestStartTime(u: TPRef): Int = stn.earliestStart(id(u))

  override protected def addConstraint(u: TPRef, v: TPRef, w: Int): Unit = stn.addConstraint(id(u), id(v), w)

  override protected def addConstraintWithID(u: TPRef, v: TPRef, w: Int, constID: ID): Unit = stn.addConstraintWithID(id(u), id(v), w, constID)

  override protected def isConstraintPossible(u: TPRef, v: TPRef, w: Int): Boolean = stn.isConstraintPossible(id(u), id(v), w)

  override def setTime(tp:TPRef, time:Int) { stn.enforceInterval(stn.start, id(tp), time, time) }
}
