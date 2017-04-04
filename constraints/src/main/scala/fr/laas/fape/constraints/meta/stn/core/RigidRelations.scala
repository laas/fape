package fr.laas.fape.constraints.meta.stn.core

import fr.laas.fape.constraints.meta.stn.variables.Timepoint

import scala.collection.mutable


class RigidRelations(val anchored: mutable.Map[Timepoint, mutable.Map[Timepoint,Int]],
                     val _anchorOf: mutable.Map[Timepoint,Timepoint]) {

  def this() = this(mutable.Map(), mutable.Map())

  override def clone() : RigidRelations = {
    val newAnchored = mutable.Map[Timepoint, mutable.Map[Timepoint,Int]]()
    for((tp,map) <- anchored)
      newAnchored.put(tp, map.clone())
    val newAnchorOf = _anchorOf.clone()
    new RigidRelations(newAnchored, newAnchorOf)
  }

  def isAnchored(tp: Timepoint) = _anchorOf.contains(tp)
  def isAnchor(tp: Timepoint) = anchored.contains(tp)
  def anchorOf(tp: Timepoint) = _anchorOf(tp)
  def distFromAnchor(tp: Timepoint) = anchored(_anchorOf(tp))(tp)
  def distToAnchor(tp: Timepoint) = -distFromAnchor(tp)
  def getTimepointsAnchoredTo(tp: Timepoint) : List[Timepoint] = anchored(tp).keys.toList

  def addAnchor(tp: Timepoint): Unit = {
    anchored(tp) = mutable.Map[Timepoint, Int]()
  }

  /** record a new rigid relation between those two timepoints.
    * At least one of them must be in the set already */
  def addRigidRelation(from: Timepoint, to: Timepoint, d: Int): Unit = {
    require(from != to)
    assert(isAnchor(from) && isAnchor(to))

    if(from.isStructural && !to.isStructural) {
      addRigidRelation(to, from, -d) // reverse to favor compiling structural timepoints
    } else {
      for (tp <- anchored(to).keys) {
        anchored(from).put(tp, d + distFromAnchor(tp))
        _anchorOf(tp) = from
      }
      anchored.remove(to)
      _anchorOf(to) = from
      anchored(from).put(to, d)
    }
  }
}
