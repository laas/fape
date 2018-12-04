package fr.laas.fape.constraints.stnu.structurals

import fr.laas.fape.anml.model.concrete.TPRef
import java.util.{HashMap => JMap}
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer


private[structurals] final class AnchorOf(val anchor: TPRef, val distFromAnchor: Int)

final class RigidRelations(private val anchored: JMap[TPRef, JMap[TPRef,Int]],
                           private val _anchorOf: ArrayBuffer[AnchorOf]) {


  def this() = this(new JMap(), new ArrayBuffer[AnchorOf]())

  override def clone() : RigidRelations = {
    val newAnchored = new JMap[TPRef, JMap[TPRef,Int]]()
    for((tp,map) <- anchored.asScala)
      newAnchored.put(tp, new JMap[TPRef,Int](map))
    new RigidRelations(newAnchored, _anchorOf.clone())
  }

  def isAnchored(tp: TPRef) = _anchorOf.size > tp.id && _anchorOf(tp.id) != null
  def anchoredTimepoints: Iterator[TPRef] = {
    _anchorOf.indices.iterator.filter(_anchorOf(_) != null).map(new TPRef(_))
  }
  def isAnchor(tp: TPRef) = anchored.containsKey(tp)
  def anchorOf(tp: TPRef) = _anchorOf(tp.id).anchor
  def distFromAnchor(tp: TPRef) = _anchorOf(tp.id).distFromAnchor
  def distToAnchor(tp: TPRef) = -distFromAnchor(tp)
  def addAnchored(tp: TPRef, anchorOf: AnchorOf): Unit = {
    while(_anchorOf.size <= tp.id) {
      _anchorOf += null
    }
    _anchorOf(tp.id) = anchorOf
  }
  def getTimepointsAnchoredTo(tp: TPRef) : List[TPRef] = anchored.get(tp).asScala.keys.toList

  def addAnchor(tp: TPRef): Unit = {
    anchored.put(tp, new JMap[TPRef, Int]())
  }

  /** record a new rigid relation between those two timepoints.
    * At least one of them must be in the set already */
  def addRigidRelation(from: TPRef, to: TPRef, d: Int): Unit = {
    require(from != to)
    assert(isAnchor(from) && isAnchor(to))

    if(from.genre.isStructural && !to.genre.isStructural) {
      addRigidRelation(to, from, -d) // reverse to favor compiling structural timepoints
    } else {
      for (tp <- anchored.get(to).asScala.keys) {
        val distFromNewAnchor = d + distFromAnchor(tp)
        anchored.get(from).put(tp, distFromNewAnchor)
        addAnchored(tp, new AnchorOf(from, distFromNewAnchor))
      }
      anchored.remove(to)
      addAnchored(to, new AnchorOf(from, d))
      anchored.get(from).put(to, d)
    }
  }
}
