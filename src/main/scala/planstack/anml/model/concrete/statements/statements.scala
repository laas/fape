package planstack.anml.model.concrete.statements

import planstack.anml.model._
import planstack.anml.model.concrete.TemporalInterval
import planstack.anml.ANMLException


/** Describes a concrete ANML statement such as `location(Rb) == l`, ...
  *
  * All variables references in those statements are global variables.
  * For the abstract version (with local variables), see [[planstack.anml.model.abs.AbstractStatement]].
  *
  * All classes descending from Statement are immutable except for their `status` field.
  * An instance should be able to exist in diverging nodes of the search space.
  *
  * @param sv State variable on which the statement applies.
  */
abstract class Statement(val sv:ParameterizedStateVariable)
  extends TemporalInterval {

  /** Value just before the statement. Throws ANMLException if the statement have none, check with needsSupport */
  def startValue : String

  /** Value just after the statement */
  def endValue : String

  /** Returns true if the statement requires an enabler for its `startValue` */
  def needsSupport : Boolean
}

/** Statement of the form `state-variable := value`
  *
  * Be aware that this statement has no start value, and will throw an exception when startValue is called.
  * @param sv State variable on which the statement applies.
  * @param value
  */
class Assignment(sv:ParameterizedStateVariable, val value:String)
  extends Statement(sv) {

  /** Throws ANMLException since an assignment has no startValue */
  def startValue = throw new ANMLException("Assignments have no start value. Check with needsSupport.")
  val endValue = value
  val needsSupport = true

  override def toString = "%s := %s".format(sv, value)
}

class Transition(sv:ParameterizedStateVariable, val from:String, val to:String)
  extends Statement(sv) {

  val startValue = from
  val endValue = to
  val needsSupport = true
  override def toString = "%s == %s :-> %s".format(sv, from, to)
}

class Persistence(sv:ParameterizedStateVariable, val value:String)
  extends Statement(sv) {

  val startValue = value
  val endValue = value
  val needsSupport = true
  override def toString = "%s == %s".format(sv, value)
}