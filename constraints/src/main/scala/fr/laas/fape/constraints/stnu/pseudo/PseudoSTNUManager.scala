package fr.laas.fape.constraints.stnu.pseudo

import fr.laas.fape.constraints.stnu._
import Controllability._
import fr.laas.fape.anml.model.concrete.TPRef
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter

protected class TConstraint[ID](val u:TPRef, val v:TPRef, val min:Int, val max:Int, val optID:Option[ID])

class PseudoSTNUManager[ID](val stn : FullSTN[ID],
                                  _tps : Array[TPRef],
                                  _ids : Array[Int],
                                  _rawConstraints : List[Constraint[ID]],
                                  _start : Option[TPRef],
                                  _end : Option[TPRef])
  extends GenSTNUManager[ID](_tps, _ids, _rawConstraints, _start, _end)
{
  def this() = this(new FullSTN[ID](), Array(), Array(), List(), None, None)
  def this(toCopy:PseudoSTNUManager[ID]) =
    this(toCopy.stn.cc(), toCopy.tps.clone(), toCopy.id.clone(), toCopy.rawConstraints, toCopy.start, toCopy.end)

  override def controllability = PSEUDO_CONTROLLABILITY

  def contingents = rawConstraints.view.filter(c => c.tipe == ElemStatus.CONTINGENT)

  override def deepCopy(): PseudoSTNUManager[ID] = new PseudoSTNUManager(this)

  override def isConsistent: Boolean = {
    stn.consistent && contingents.forall(c => stn.isMinDelayPossible(id(c.u.id), id(c.v.id), c.d))
  }

  override def exportToDotFile(filename: String, printer: NodeEdgePrinter[Object, Object, LabeledEdge[Object, Object]]): Unit =
    println("Warning: this STNUManager can not be exported to a dot file")

  override protected def isConstraintPossible(u: Int, v: Int, w: Int): Boolean = stn.isConstraintPossible(u, v, w)

  /** If there is a contingent constraint [min, max] between those two timepoints, it returns
    * Some((min, max).
    * Otherwise, None is returned.
    */
  override def contingentDelay(from: TPRef, to: TPRef): Option[(Integer, Integer)] = {
    val min = contingents.find(c => c.u == to && c.v == from).map(c => -c.d)
    val max = contingents.find(c => c.u == from && c.v == to).map(c => c.d)

    if(min.nonEmpty && max.nonEmpty)
      Some((min.get :Integer, max.get :Integer))
    else
      None
  }



  override protected def commitContingent(u: Int, v: Int, d: Int, optID: Option[ID]): Unit =
    // simple commit a controllable constraint, the contingency will be checked in isConsistent
    commitConstraint(u, v, d, optID)

  override protected def commitConstraint(u: Int, v: Int, w: Int, optID: Option[ID]): Unit =
    optID match {
      case Some(id) => stn.addConstraintWithID(u, v, w, id)
      case None => stn.addConstraint(u, v, w)
    }

  /** Returns the latest time for the time point with id u */
  override protected def latestStart(u: Int): Int = stn.latestStart(u)

  /** should remove a constraint from the underlying STNU */
  override protected def performRemoveConstraintWithID(id: ID): Boolean =
    stn.removeConstraintsWithID(id)

  /** Returns the earliest time for the time point with id u */
  override protected def earliestStart(u: Int): Int = stn.earliestStart(u)

  private def dist(u :TPRef, v:TPRef) : Int = {
    val (src, addDelay) =
      if(u.isVirtual) u.attachmentToReal
      else (u, 0)
    val (dst, subDelay) =
      if(v.isVirtual) v.attachmentToReal
      else (v, 0)
    (- addDelay) + stn.maxDelay(id(src.id), id(dst.id)) + subDelay
  }

  override def getMinDelay(u: TPRef, v: TPRef): Int = - dist(v, u)

  override def getMaxDelay(u: TPRef, v: TPRef): Int = dist(u,v)
}
