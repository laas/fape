package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.{LVarRef, Type}

case class IRSimpleVar(name:String, typ:Type) extends IRVar {
  def simpleVariable = true
  override def id: String = name
  override def getAllVars: Set[LVarRef] = Set(this)
  override def asANML: String = name
  override def toString = asANML
}
