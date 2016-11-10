package fr.laas.fape.constraints.stnu.structurals

import fr.laas.fape.anml.model.concrete.TPRef

import scala.collection.mutable


class RigidRelations(val anchored: mutable.Map[TPRef, mutable.Map[TPRef,Int]],
                     val _anchorOf: mutable.Map[TPRef,TPRef]) {

  def this() = this(mutable.Map(), mutable.Map())

  override def clone() : RigidRelations = {
    val newAnchored = mutable.Map[TPRef, mutable.Map[TPRef,Int]]()
    for((tp,map) <- anchored)
      newAnchored.put(tp, map.clone())
    val newAnchorOf = _anchorOf.clone()
    new RigidRelations(newAnchored, newAnchorOf)
  }

  def isAnchored(tp: TPRef) = _anchorOf.contains(tp)
  def isAnchor(tp: TPRef) = anchored.contains(tp)
  def anchorOf(tp: TPRef) = _anchorOf(tp)
  def distFromAnchor(tp: TPRef) = anchored(_anchorOf(tp))(tp)
  def distToAnchor(tp: TPRef) = -distFromAnchor(tp)

  def addAnchor(tp: TPRef): Unit = {
    anchored(tp) = mutable.Map[TPRef, Int]()
  }

  /** record a new rigid relation between those two timepoints.
    * At least one of them must be in the set already */
  def addRigidRelation(from: TPRef, to: TPRef, d: Int): Unit = {
    require(from != to)
    assert(isAnchor(from) && isAnchor(to))

    if(from.genre.isStructural && !to.genre.isStructural) {
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
