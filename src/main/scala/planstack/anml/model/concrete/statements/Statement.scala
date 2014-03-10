package planstack.anml.model.concrete.statements

import planstack.anml.model._


abstract class Statement(val context:Context, val sv:ParameterizedStateVariable) extends TemporalInterval {
}

class Assignment(context:Context, sv:ParameterizedStateVariable, val value:String)
  extends Statement(context, sv)
{
  override def toString = "%s := %s".format(sv, value)
}

class Transition(context:Context, sv:ParameterizedStateVariable, val from:String, val to:String)
  extends Statement(context, sv)
{
  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class Persistence(context:Context, sv:ParameterizedStateVariable, val value:String)
  extends Statement(context, sv)
{
  override def toString = "%s == %s".format(sv, value)
}