package planstack.anml.model.abs

import planstack.anml.parser
import planstack.anml.model.{LActRef}
import planstack.anml.model.abs.time.AbstractTimepointRef


class AbstractTimePointExtractor(val extractor:String, val id:String) {
  require(Set("start", "end").contains(extractor))

  override def toString = "%s(%s)".format(extractor, id)
}

class AbstractTemporalConstraint(
    val tp1:AbstractTimepointRef,
    val op:String,
    val tp2:AbstractTimepointRef,
    val plus:Integer) {

  override def toString = "%s %s %s + %s".format(tp1, op, tp2, plus)
}

object AbstractTemporalConstraint {

  def apply(parsed:parser.TemporalConstraint) : AbstractTemporalConstraint = {
    new AbstractTemporalConstraint(
      AbstractTimepointRef(parsed.tp1),
      parsed.operator,
      AbstractTimepointRef(parsed.tp2),
      parsed.delta
    )
  }

  /** Returns a new Abstract temporal constraint enforcing action1 to be before action2
    *
    * @param action1 local id of an action
    * @param action2 local id of an action
    * @return
    */
  def before(action1:LActRef, action2:LActRef) =
    new AbstractTemporalConstraint(
      new AbstractTimepointRef("end", action1),
      "<",
      new AbstractTimepointRef("start", action2),
      0)
}