package planstack.constraints.stnu

import net.openhft.koloboke.collect.map.hash.{HashIntIntMap, HashIntObjMap}
import planstack.UniquelyIdentified
import planstack.constraints.Kolokobe
import planstack.constraints.stnu.ElemStatus._
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter
import planstack.structures.IList
import planstack.structures.Converters._
import scala.language.implicitConversions

class STNUManager[TPRef <: UniquelyIdentified,ID](val stnu : ISTNU[ID],
                            _tps : HashIntObjMap[TimePoint[TPRef]],
                            _ids : HashIntIntMap,
                            _rawConstraints : List[Constraint[TPRef,ID]],
                            _start : Option[TPRef],
                            _end : Option[TPRef])
  extends GenSTNUManager[TPRef,ID](_tps, _ids, _rawConstraints, _start, _end)
{
  // could use FastIDC as well
  def this() = this(new MMV[ID](), Kolokobe.getIntObjMap[TimePoint[TPRef]], Kolokobe.getIntIntMap, List(), None, None)

  implicit def TPRef2stnID(tp : TPRef) : Int = id.get(tp.id)

  override def stn = stnu

  override def controllability = stnu.controllability

  /** Makes an independent clone of this STN. */
  override def deepCopy(): STNUManager[TPRef, ID] =
    new STNUManager[TPRef,ID](stnu.cc(), Kolokobe.clone(tps), Kolokobe.clone(id), rawConstraints, start, end)

  /** Returns true if the STN is consistent (might trigger a propagation */
  override def isConsistent(): Boolean = stnu.consistent

  override protected def isConstraintPossible(u: Int, v: Int, w: Int): Boolean = stnu.isConstraintPossible(u, v, w)

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit =
    stnu.writeToDotFile(filename)

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

  override def getMinDelay(u: TPRef, v: TPRef): Int = ???

  override def getMaxDelay(u: TPRef, v: TPRef): Int = ???
}
