package fr.laas.fape.constraints.stnu.structurals

import fr.laas.fape.anml.model.concrete.TPRef

import scala.collection.mutable

private[structurals] final class AnchorOf(val anchor: TPRef, val distFromAnchor: Int)

final class RigidRelations(private val anchored: mutable.Map[TPRef, mutable.Map[TPRef,Int]],
                           private val _anchorOf: mutable.Map[TPRef,AnchorOf]) {



  def this() = this(mutable.Map(), mutable.Map())

  override def clone() : RigidRelations = {
    val newAnchored = mutable.Map[TPRef, mutable.Map[TPRef,Int]]()
    for((tp,map) <- anchored)
      newAnchored.put(tp, map.clone())
    val newAnchorOf = _anchorOf.clone()
    new RigidRelations(newAnchored, newAnchorOf)
  }

  def isAnchored(tp: TPRef) = _anchorOf.contains(tp)
  def anchoredTimepoints: Iterator[TPRef] = _anchorOf.keysIterator
  def isAnchor(tp: TPRef) = anchored.contains(tp)
  def anchorOf(tp: TPRef) = _anchorOf(tp).anchor
  def distFromAnchor(tp: TPRef) = _anchorOf(tp).distFromAnchor
  def distToAnchor(tp: TPRef) = -distFromAnchor(tp)
  def getTimepointsAnchoredTo(tp: TPRef) : List[TPRef] = anchored(tp).keys.toList

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
        val distFromNewAnchor = d + distFromAnchor(tp)
        anchored(from).put(tp, distFromNewAnchor)
        _anchorOf(tp) = new AnchorOf(from, distFromNewAnchor)
      }
      anchored.remove(to)
      _anchorOf(to) = new AnchorOf(from, d)
      anchored(from).put(to, d)
    }
  }
}
