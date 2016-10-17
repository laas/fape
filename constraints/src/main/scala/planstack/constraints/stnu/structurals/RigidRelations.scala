package planstack.constraints.stnu.structurals

import planstack.anml.model.concrete.TPRef

import scala.collection.mutable


class RigidRelations(val anchored: mutable.Map[TPRef, mutable.Map[TPRef,Int]],
                     val _anchorOf: mutable.Map[TPRef,TPRef]) {

  def isAnchored(tp: TPRef) = _anchorOf.contains(tp)
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

    if(isAnchor(from) && isAnchor(to)) { // merge
      for(tp <- anchored(to).keys) {
        anchored(from).put(tp, d + distFromAnchor(tp))
        _anchorOf(tp) = from
      }
      anchored.remove(to)
    } else if(isAnchor(from)) {
      assert(!isAnchored(to))
      // integrate "to" in the anchored set of "from"
      _anchorOf(to) = from
      anchored(from).put(to, d)
    } else if(isAnchor(to)) {
      addRigidRelation(to, from, -d) // reverse to avoid code duplication
    } else {
      assert(!isAnchored(from) && ! isAnchored(to))
      // mark "from" as a new anchor and place "to" in its anchored set
      anchored(from) = mutable.Map[TPRef,Int]()
      anchored(from).put(to, d)
      _anchorOf(to) = from
    }
  }




}
