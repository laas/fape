package fr.laas.fape.anml.model.ir

import fr.laas.fape.anml.model.Function

trait IRFunction extends IRExpression {
  def func:Function
  def args:List[IRVar]
}

object IRFunction {
  def build(func:Function, args:List[IRVar]) = func.isConstant match {
    case true => IRConstantExpression(func, args)
    case false => IRTimedFunction(func, args)
  }
}