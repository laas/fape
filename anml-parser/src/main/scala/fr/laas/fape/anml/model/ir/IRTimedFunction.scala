package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.Function

case class IRTimedFunction(func:Function, args:List[IRVar]) extends IRFunction {
  require(!isConstant)
  def typ = func.valueType
  def isConstant = func.isConstant
}
