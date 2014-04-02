package planstack.anml.model.concrete

import planstack.anml.model.{AnmlProblem, LActRef, Context}
import planstack.anml.model.abs.{AbstractTemporalConstraint}
import planstack.anml.model.concrete.time.TimepointRef




case class TemporalConstraint(tp1:TPRef, op:String, tp2:TPRef, plus:Integer) {
  require(op == "<" || op == "=")

  override def toString = "%s %s %s + %s".format(tp1, op, tp2, plus)
}

object TemporalConstraint {

  def apply(pb:AnmlProblem, context:Context, abs:AbstractTemporalConstraint) = {
    new TemporalConstraint(TimepointRef(pb, context, abs.tp1), abs.op, TimepointRef(pb, context, abs.tp2), abs.plus)
  }
}