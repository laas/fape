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

  def isAnchored(tp: TPRef) = { assert(!_anchorOf.contains(tp) || tp.genre.isStructural); _anchorOf.contains(tp) }
  def isAnchor(tp: TPRef) = anchored.contains(tp)
  def anchorOf(tp: TPRef) = _anchorOf(tp)
  def distFromAnchor(tp: TPRef) = anchored(_anchorOf(tp))(tp)
  def distToAnchor(tp: TPRef) = -distFromAnchor(tp)

  /** record a new rigid relation between those two timepoints.
    * At least one of them must be in the set already */
  def addRigidRelation(from: TPRef, to: TPRef, d: Int): Unit = {
    assert(from != to)
    assert(!isAnchored(from))
    assert(!isAnchored(to))
    assert(from.genre.isStructural || to.genre.isStructural)

    if(isAnchor(from) && isAnchor(to)) { // merge
      if(!to.genre.isStructural) {
        addRigidRelation(to, from, -d)
      } else {
        assert(to.genre.isStructural, "About to compile away a non structural timepoint")
        for (tp <- anchored(to).keys) {
          anchored(from).put(tp, d + distFromAnchor(tp))
          _anchorOf(tp) = from
        }
        anchored.remove(to)
        _anchorOf(to) = from
        anchored(from).put(to, d)
      }
    } else if(isAnchor(from)) {
      assert(!isAnchored(to))
      assert(to.genre.isStructural, "About to compile away a non structural timepoint")
      // integrate "to" in the anchored set of "from"
      _anchorOf(to) = from
      anchored(from).put(to, d)
    } else if(isAnchor(to)) {
      addRigidRelation(to, from, -d) // reverse to avoid code duplication
    } else {
      if(!to.genre.isStructural) {
        addRigidRelation(to, from, -d)
      } else {
        assert(!isAnchored(from) && !isAnchored(to))
        assert(to.genre.isStructural, "About to compile away a non structural timepoint")
        // mark "from" as a new anchor and place "to" in its anchored set
        anchored(from) = mutable.Map[TPRef, Int]()
        anchored(from).put(to, d)
        _anchorOf(to) = from
      }
    }
  }




}
