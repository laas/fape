package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.{ResourceStatement, LogStatement, Statement, TemporalStatement}
import collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import java.util

/** A state modifier decribes modifications to be made to plan.
  *
  * Notable classes implementing it are [[planstack.anml.model.concrete.Action]]
  * and [[planstack.anml.model.concrete.Decomposition]].
  * Updates to a problem (such as the happening of exogeneous events) are also encoded as StateModifiers
  * in [[planstack.anml.model.AnmlProblem]].
  *
  * Components:
  *  - `vars`: global variables that need to be declared for applying the modifier.
  *  - `statements`: individual statements depicting a condition of a change on a state variable. Those
  *    come from the effects/preconditions of actions, conditions on decomposition or exogeneous events.
  *  - `actions`: actions to be inserted in the plan. Note that actions are StateModifiers themselves.
  *
  */
trait StateModifier {

  /** A temporal interval in which the modifier is applied. For instance, if this StateModifier refers to
    * an action, the container would refer to the [start, end] interval of this action.
    * ANML temporal annotations such as [start] refer to this temporal interval.
    * Note that time points might appear outside this interval, for instance with the annotations
    * [start-10], [end+10] or [7].
    */
  def container : TemporalInterval

  /** Temporally annotated statements to be inserted in the plan */
  def statements : java.util.List[Statement]

  /** Returns all logical statements */
  def logStatements : java.util.List[LogStatement] = seqAsJavaList(statements.filter(_.isInstanceOf[LogStatement]).map(_.asInstanceOf[LogStatement]))

  /** Returns all logical statements */
  def resourceStatements : java.util.List[ResourceStatement] = seqAsJavaList(statements.filter(_.isInstanceOf[ResourceStatement]).map(_.asInstanceOf[ResourceStatement]))

  /** Actions to be inserted in the plan */
  def actions : java.util.List[Action]

  /** Actions conditions that must be fulfilled by the plan.
    *
    * An action condition has an action name, a set of parameters and two timepoints.
    * It can be fulfilled/supported by an action with the same whose parameters and
    * time points are equal to those of the action condition.
    */
  def actionConditions : java.util.List[ActionCondition]

  /** (Type, Reference) of global variables to be declared */
  def vars : java.util.List[Pair[String, VarRef]]

  /** All problem instances to be declared
    * Problem instances are typically a global variable with a domain containing only one value (itself).
    */
  def instances : java.util.List[String] = Nil

  def temporalConstraints : java.util.List[TemporalConstraint]
  }

class BaseStateModifier(val container: TemporalInterval) extends StateModifier {

  val statements = new util.LinkedList[Statement]()
  val actions = new util.LinkedList[Action]()
  val actionConditions = new util.LinkedList[ActionCondition]()
  val vars = new util.LinkedList[Pair[String, VarRef]]()
  override val instances = new util.LinkedList[String]()
  val temporalConstraints = new util.LinkedList[TemporalConstraint]()
}