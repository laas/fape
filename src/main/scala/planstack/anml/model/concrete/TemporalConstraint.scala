package planstack.anml.model.concrete

import planstack.anml.model.Context
import planstack.anml.model.abs.{AbstractTimePointExtractor, AbstractTemporalConstraint}


class TimePointExtractor(val extractor:String, val id:String) {
  assert(Set("startOf", "endOf").contains(extractor))

  override def toString = "%s(%s)".format(extractor, id)
}

object TimePointExtractor {

  def apply(context:Context, abs:AbstractTimePointExtractor) = new TimePointExtractor(abs.extractor, context.getActionID(abs.id))
}

class TemporalConstraint(val tp1:TimePointExtractor, val op:String, val tp2:TimePointExtractor, val plus:Integer) {
  require(op == "<" || op == "=")

  override def toString = "%s %s %s + %s".format(tp1, op, tp2, plus)
}

object TemporalConstraint {

  def apply(context:Context, abs:AbstractTemporalConstraint) = {
    new TemporalConstraint(TimePointExtractor(context, abs.tp1), abs.op, TimePointExtractor(context, abs.tp2), abs.plus)
  }
}