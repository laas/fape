package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.TemporalStatement
import collection.JavaConversions._
import planstack.anml.model.VarRef

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
  def statements : Seq[TemporalStatement]

  /** Java friendly version of: [[planstack.anml.model.concrete.StateModifier#statements]] */
  def jStatements = seqAsJavaList(statements)

  /** Actions to be inserted in the plan */
  def actions : Seq[Action]
  /** Actions to be inserted in the plan */
  def jActions = seqAsJavaList(actions)

  /** (Type, Reference) of global variables to be declared */
  def vars : Seq[Pair[String, VarRef]]
  /** (Type, Reference) of global variables to be declared */
  def jVars = seqAsJavaList(vars)

  /** All problem instances to be declared
    * Problem instances are typically a global variable with a domain containing only one value (itself).
    */
  def instances : Seq[String] = Nil
  /** All problem instances to be declared */
  def jInstances = seqAsJavaList(instances)

  def temporalConstraints : Seq[TemporalConstraint]
  def jTemporalConstraints = seqAsJavaList(temporalConstraints)

}

class BaseStateModifier(
     val container: TemporalInterval,
     val statements: Seq[TemporalStatement],
     val actions: Seq[Action],
     val vars: Seq[Pair[String, VarRef]],
     override val instances: Seq[String])
  extends StateModifier {

  val temporalConstraints = Nil

  def withStatements(addStatements:TemporalStatement*) = new BaseStateModifier(container, statements ++ addStatements, actions, vars, instances)

  def withActions(addActions:Action*) = new BaseStateModifier(container, statements, actions ++ addActions, vars, instances)

  def withVariables(addVars:Pair[String, VarRef]*) = new BaseStateModifier(container, statements, actions, vars ++ addVars, instances)

  def withInstances(addInstances:String*) = new BaseStateModifier(container, statements, actions, vars, instances ++ addInstances)
}