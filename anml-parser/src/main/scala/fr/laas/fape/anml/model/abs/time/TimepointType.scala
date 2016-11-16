package fr.laas.fape.anml.model.abs.time

import TimepointTypeEnum._

class TimepointType {
  private var typ: TimepointTypeEnum = STRUCTURAL_BY_DEFAULT
  def isContingent = typ == CONTINGENT
  def isStructural = typ == STRUCTURAL || typ == STRUCTURAL_BY_DEFAULT
  def isNecessarilyStructural = typ == STRUCTURAL
  def isDispatchable = typ == DISPATCHABLE || typ == DISPATCHABLE_BY_DEFAULT
  def setType(newType: TimepointTypeEnum): Unit = {
    if(typ == STRUCTURAL_BY_DEFAULT || typ == DISPATCHABLE_BY_DEFAULT)
      typ = newType
    else if(newType != STRUCTURAL_BY_DEFAULT && newType != DISPATCHABLE_BY_DEFAULT)
      assert(typ == newType, s"Forcing a new type $newType on a type with a (non-default) type $typ")
  }

  override def toString = typ.toString
}
