package fr.laas.fape.anml.model.ir

case class IRTriStatement(e1:IRExpression, op:String, e2:IRExpression, op2:String, e3:IRExpression, id:String) extends IRStatement
