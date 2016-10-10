package planstack.constraints.stn

import planstack.anml.model.concrete.TPRef

import scala.collection.mutable

class RigidRelations(val anchors: mutable.Map[TPRef, mutable.Map[TPRef,Int]],
                     val anchored: mutable.Map[TPRef,TPRef]) {

  def isAnchored(tp: TPRef) = anchored.contains(tp)
  def isAnchor(tp: TPRef) = anchors.contains(tp)
  def anchorOf(tp: TPRef) = anchored(tp)
  def distFromAnchor(tp: TPRef) = anchors(anchorOf(tp))(tp)

  /** record a new rigid relation between those two timepoints.
    * At least one of them must be in the set already */
  def addRigidRelation(from: TPRef, to: TPRef, d: Int): Unit = {
    if(isAnchor(from) && isAnchor(to)) {
      // merge
    } else if(isAnchor(from)) {
      assert(!isAnchored(to))
      // integrate
    } else if(isAnchor(to)) {
      addRigidRelation(to, from, -d) // reverse to avoid code duplication
    } else {
      assert(!isAnchored(from) && ! isAnchored(to))
      // create new set
      anchors(from) = mutable.Map[TPRef,Int]()
      anchors(from).put(to, d)
      anchored(to) = from
    }
  }




}
