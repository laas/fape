package planstack.constraints.stnu

import planstack.UniquelyIdentified
import planstack.constraints.stnu.ElemStatus._
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter
import planstack.structures.IList
import planstack.structures.Converters._
import scala.language.implicitConversions

class STNUManager[TPRef <: UniquelyIdentified,ID](val stnu : ISTNU[ID],
                            _dispatchableVars : Set[TPRef],
                            _contingentVars : Set[TPRef],
                            _ids : Map[TPRef,Int],
                            _virtuals : Map[TPRef, Option[(TPRef,Int)]],
                            _rawConstraints : List[Constraint[TPRef,ID]],
                            _start : Option[TPRef],
                            _end : Option[TPRef])
  extends GenSTNUManager[TPRef,ID](_virtuals, _ids, _dispatchableVars, _contingentVars, _rawConstraints, _start, _end)
{
  // could use FastIDC as well
  def this() = this(new MMV[ID](), Set(), Set(), Map(), Map(), List(), None, None)

  implicit def TPRef2stnID(tp : TPRef) : Int = id(tp)

  override def controllability = stnu.controllability

  /** Makes an independent clone of this STN. */
  override def deepCopy(): STNUManager[TPRef, ID] =
    new STNUManager[TPRef,ID](stnu.cc(), dispatchableTPs, contingentTPs, id, virtualTPs, rawConstraints, start, end)

  /** Returns true if the STN is consistent (might trigger a propagation */
  override def isConsistent(): Boolean = stnu.consistent

  override protected def isConstraintPossible(u: Int, v: Int, w: Int): Boolean = stnu.isConstraintPossible(u, v, w)

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit =
    stnu.writeToDotFile(filename)

  /** Records a new time point in the STN. Returns its ID (which might change) */
  override def recordTimePoint(tp: TPRef): Int = {
    assert(!id.contains(tp))
    id += ((tp, stnu.addVar()))
    id(tp)
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


  override def contingentDelay(from:TPRef, to:TPRef) = stnu.getContingentDelay(from, to) match {
    case Some((min, max)) => Some((min:Integer, max:Integer))
    case None => None
  }

  override protected def commitContingent(u: Int, v: Int, d: Int, optID: Option[ID]): Unit =
    optID match {
      case Some(id) => stnu.addContingentWithID(u, v, d, id)
      case None => stnu.addContingent(u, v, d)
    }

  override protected def commitConstraint(u: Int, v: Int, w: Int, optID: Option[ID]): Unit =
    optID match {
      case Some(id) => stnu.addConstraintWithID(u, v, w, id)
      case None => stnu.addConstraint(u, v, w)
    }

  /** Returns the latest time for the time point with id u */
  override protected def latestStart(u: Int): Int = stnu.latestStart(u)

  /** should remove a constraint from the underlying STNU */
  override protected def performRemoveConstraintWithID(id: ID): Boolean =
    stnu.removeConstraintsWithID(id)

  /** Returns the earliest time for the time point with id u */
  override protected def earliestStart(u: Int): Int = stnu.earliestStart(u)
}
