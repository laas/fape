package fr.laas.fape.anml.model.ir

case class IRBiStatement(e1:IRExpression, op:String, e2:IRExpression, id:String) extends IRStatement {
  override def toString = s"$id: $e1 $op $e2"
}
