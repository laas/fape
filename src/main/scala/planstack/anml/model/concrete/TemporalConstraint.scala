package planstack.anml.model.concrete

import planstack.anml.model.{LActRef, Context}
import planstack.anml.model.abs.{AbstractTemporalConstraint}
import planstack.anml.model.abs.time.AbstractTimepointRef
import planstack.anml.ANMLException
import planstack.anml.model.concrete.time.TimepointRef




class TemporalConstraint(val tp1:TimepointRef, val op:String, val tp2:TimepointRef, val plus:Integer) {
  require(op == "<" || op == "=")

  override def toString = "%s %s %s + %s".format(tp1, op, tp2, plus)
}

object TemporalConstraint {

  def apply(context:Context, abs:AbstractTemporalConstraint) = {
    new TemporalConstraint(TimepointRef(context, abs.tp1), abs.op, TimepointRef(context, abs.tp2), abs.plus)
  }
}