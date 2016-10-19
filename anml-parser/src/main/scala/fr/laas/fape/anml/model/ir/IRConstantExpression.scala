package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.{Function, LVarRef, Type}

case class IRConstantExpression(func:Function, args:List[IRVar]) extends IRVar with IRFunction {
  require(func.isConstant)
  def isConstant = func.isConstant
  def simpleVariable = false
  def id = asANML
  override def typ: Type = func.valueType
  override def getAllVars: Set[LVarRef] = args.flatMap(a => a.getAllVars).toSet + this
  override def asANML: String = s"${func.name}(${args.map(_.asANML).mkString(",")})"
  override def toString = asANML
}
