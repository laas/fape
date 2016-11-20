package fr.laas.fape.anml.model.concrete.statements

import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.model.ParameterizedStateVariable
import fr.laas.fape.anml.model.abs.statements.AbstractStatement
import fr.laas.fape.anml.model.concrete.TemporalInterval
import fr.laas.fape.anml.model._
import fr.laas.fape.anml.model.concrete._


/** Describes a concrete ANML statement such as `location(Rb) == l`, ...
  *
  * All variables references in those statements are global variables.
  * For the abstract version (with local variables), see [[AbstractStatement]].
  *
  * All classes descending from Statement are immutable except for their `status` field.
  * An instance should be able to exist in diverging nodes of the search space.
  *
  * @param sv State variable on which the statement applies.
  * @param container The chronicle in which this statement appears.
  */
abstract class Statement(val sv:ParameterizedStateVariable, val container: Chronicle)
  extends TemporalInterval

/** Logical statement that refers to binding constraints on state variables values.
  *
  * More specifically, a logical statement is one of:
  *  - [[fr.laas.fape.anml.model.concrete.statements.Assignment]]: `stateVariable(_) := x;`
  *  - [[fr.laas.fape.anml.model.concrete.statements.Persistence]] `stateVariable(_) == y;`
  *  - [[fr.laas.fape.anml.model.concrete.statements.Transition]]: `stateVariable == w :-> z;`
  *
  * See [[fr.laas.fape.anml.model.concrete.statements.Statement]] for more details on statements in general.
  * @param sv State variable on which the statement applies.
  */
abstract class LogStatement(sv:ParameterizedStateVariable, container:Chronicle) extends Statement(sv, container) with VariableUser {
  require(sv.func.isInstanceOf[SymFunction], "Error: this Logical statement is not applied to a " +
    "symbolic function: "+this)

  /** Value just before the statement. Throws ANMLException if the statement have none, check with needsSupport */
  def startValue : VarRef

  /** Value just after the statement */
  def endValue : VarRef

  /** Returns true if the statement requires an enabler for its `startValue` */
  def needsSupport : Boolean

  /** Returns true if the statements will change the value of the state variable (i.e. transition or assignment) */
  def isChange : Boolean
}

/** Statement of the form `state-variable := value`
  *
  * Be aware that this statement has no start value, and will throw an exception when startValue is called.
  * @param sv State variable on which the statement applies.
  * @param value
  */
class Assignment(sv:ParameterizedStateVariable, val value:VarRef, container:Chronicle, refCounter: RefCounter)
  extends LogStatement(sv, container) {

  override val start : TPRef = new TPRef(refCounter)
  override val end : TPRef = new TPRef(refCounter)

  /** Throws ANMLException since an assignment has no startValue */
  def startValue = throw new ANMLException("Assignments have no start value. Check with needsSupport.")
  val endValue = value
  val needsSupport = false

  override def toString = "%s := %s".format(sv, value)

  /** Returns true if the statements will change the value of the state variable (i.e. transition or assignment) */
  override def isChange: Boolean = true

  override def usedVariables: Set[Variable] = sv.usedVariables + value
}

/** Statement of the form `stateVariable(arg1, arg2, ...) == x :-> y;` referring to the change of the state variable from
  * value x to value y;
  *
  * @param sv State variable on which the statement applies.
  * @param from Value of the state variable before the statement.
  * @param to Value of the state variable after the statement.
  */
class Transition(sv:ParameterizedStateVariable, val from:VarRef, val to:VarRef, container:Chronicle, refCounter: RefCounter)
  extends LogStatement(sv, container) {

  override val start : TPRef = new TPRef(refCounter)
  override val end : TPRef = new TPRef(refCounter)

  val startValue = from
  val endValue = to
  val needsSupport = true
  override def toString = "%s == %s :-> %s".format(sv, from, to)

  /** Returns true if the statements will change the value of the state variable (i.e. transition or assignment) */
  override def isChange: Boolean = true

  override def usedVariables: Set[Variable] = sv.usedVariables + from + to
}

/** Statement of the form `stateVariable(arg1, arg2, ...) == x;` refering to a persistence of the state variable at value x
  * during the time interval of the statement.
  *
  * @param sv State variable on which the statement applies.
  * @param value Value of the state variable during the statement.
  */
class Persistence(sv:ParameterizedStateVariable, val value:VarRef, container:Chronicle, refCounter: RefCounter)
  extends LogStatement(sv, container) {

  override val start : TPRef = new TPRef(refCounter)
  override val end : TPRef = new TPRef(refCounter)

  val startValue = value
  val endValue = value
  val needsSupport = true
  override def toString = "%s == %s".format(sv, value)

  /** Returns true if the statements will change the value of the state variable (i.e. transition or assignment) */
  override def isChange: Boolean = false

  override def usedVariables: Set[Variable] = sv.usedVariables + value
}