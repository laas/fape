package fr.laas.fape.constraints.stnu

import fr.laas.fape.anml.model.concrete.TPRef
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter

import scala.language.implicitConversions

class STNUManager[ID](val stnu : CoreSTNU[ID],
                      _tps : Array[TPRef],
                      _ids : Array[Int],
                      _rawConstraints : List[Constraint[ID]],
                      _start : Option[TPRef],
                      _end : Option[TPRef])
  extends GenSTNUManager[ID](_tps, _ids, _rawConstraints, _start, _end)
{
  // could use FastIDC as well
  def this() = this(new MMV[ID](), Array(), Array(), List(), None, None)

  implicit def TPRef2stnID(tp : TPRef) : Int = id(tp.id)

  override def stn = stnu

  override def controllability = stnu.controllability

  /** Makes an independent clone of this STN. */
  override def deepCopy(): STNUManager[ID] =
    new STNUManager[ID](stnu.cc(), tps.clone(), id.clone(), rawConstraints, start, end)

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
