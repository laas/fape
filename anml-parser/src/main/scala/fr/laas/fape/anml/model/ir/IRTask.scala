package fr.laas.fape.anml.model.ir

case class IRTask(name:String, args:List[IRVar]) extends IRExpression
