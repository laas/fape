package planstack.constraints.stnu

import net.openhft.koloboke.collect.map.hash._
import net.openhft.koloboke.collect.map.{IntIntMap, IntObjMap}
import planstack.UniquelyIdentified
import planstack.constraints.Kolokobe
import planstack.constraints.stn.{STNIncBellmanFord, ISTN}
import planstack.constraints.stnu.ElemStatus._
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter
import planstack.structures.IList
import planstack.structures.Converters._
import scala.language.implicitConversions

class MinimalSTNUManager[TPRef <: UniquelyIdentified,ID](val stn:ISTN[ID],
                                   _tps: HashIntObjMap[TimePoint[TPRef]],
                                   _ids : HashIntIntMap,
                                   _rawConstraints : List[Constraint[TPRef,ID]],
                                   _start : Option[TPRef],
                                   _end : Option[TPRef])
  extends GenSTNUManager[TPRef,ID](_tps, _ids, _rawConstraints, _start, _end)
{
  def this() = this(new STNIncBellmanFord[ID](), Kolokobe.getIntObjMap[TimePoint[TPRef]], Kolokobe.getIntIntMap, List(), None, None)

  /** Makes an independent clone of this STN. */
  override def deepCopy(): MinimalSTNUManager[TPRef, ID] =
    new MinimalSTNUManager(stn.cc(), Kolokobe.clone(tps), Kolokobe.clone(id), rawConstraints, start, end)

  override def controllability: Controllability = Controllability.STN_CONSISTENCY

  override def commitContingent(u:Int, v:Int, d:Int, optID:Option[ID]): Unit = {
    optID match {
      case Some(cID) => stn.enforceMaxDelayWithID(u, v, d, cID)
      case None => stn.enforceMaxDelay(u, v, d)
    }
  }

  /** Returns the maximal time from the start of the STN to u */
  override def latestStart(u: Int): Int = stn.latestStart(u)

  /** Removes all constraints that were recorded with this id */
  override protected def performRemoveConstraintWithID(id: ID): Boolean = {
    stn.removeConstraintsWithID(id)
}

  /** Returns true if the STN is consistent (might trigger a propagation */
  override def isConsistent: Boolean = stn.consistent


  override protected def isConstraintPossible(u: Int, v: Int, w: Int): Boolean =
    stn.isConstraintPossible(u, v, w)

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit =
  stn.writeToDotFile(filename)


  /** Returns the minimal time from the start of the STN to u */
  override def earliestStart(u: Int): Int =
    stn.earliestStart(u)

  /** If there is a contingent constraint [min, max] between those two timepoints, it returns
    * Some((min, max).
    * Otherwise, None is returned.
    */
  override def contingentDelay(from: TPRef, to: TPRef): Option[(Integer, Integer)] = {
    val min = rawConstraints.find(c => c.tipe == CONTINGENT && c.u == to && c.v == from).map(-_.d)
    val max = rawConstraints.find(c => c.tipe == CONTINGENT && c.u == from && c.v == to).map(_.d)
    if(min.nonEmpty && max.nonEmpty)
      Some((min.get :Integer, max.get :Integer))
    else if(min.isEmpty && max.isEmpty)
      None
    else
      throw new RuntimeException("Contingent constraint does not look symmetrical")
  }

  override protected def commitConstraint(u: Int, v: Int, w: Int, optID: Option[ID]): Unit =
    optID match {
      case Some(id) => stn.addConstraintWithID(u, v, w, id)
      case None => stn.addConstraint(u, v, w)
    }

  override def getMinDelay(u: TPRef, v: TPRef): Int = ???

  override def getMaxDelay(u: TPRef, v: TPRef): Int = ???
}
