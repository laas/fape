package planstack.anml.model.abs


class AbstractTimePointExtractor(val extractor:String, val id:String) {
  assert(Set("startOf", "endOf").contains(extractor))

  override def toString = "%s(%s)".format(extractor, id)
}

class AbstractTemporalConstraint(
    val tp1:AbstractTimePointExtractor,
    val op:String,
    val tp2:AbstractTimePointExtractor,
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
  def before(action1:String, action2:String) =
    new AbstractTemporalConstraint(
      new AbstractTimePointExtractor("endOf", action1),
      "<",
      new AbstractTimePointExtractor("startOf", action2),
      0)
}