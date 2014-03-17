package planstack.anml.model.abs

import planstack.anml.model.{LActRef, AbstractTimepointRef}


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