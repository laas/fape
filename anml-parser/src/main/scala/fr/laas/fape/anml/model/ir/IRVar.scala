package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.{LVarRef, VarContainer, Type}

trait IRVar extends IRExpression with LVarRef with VarContainer {
  def typ: Type
  def simpleVariable : Boolean
}
