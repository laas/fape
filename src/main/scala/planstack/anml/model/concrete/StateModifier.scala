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

  /** Temporally annotated statements to be inserted in the plan */
  def statements : Seq[TemporalStatement]
  def jStatements = seqAsJavaList(statements)

  /** Actions to be inserted in the plan */
  def actions : Seq[Action]

  /** (Type, Name) of global variables to be declared */
  def vars : Seq[Pair[String, VarRef]]

  def temporalConstraints : Seq[TemporalConstraint]

}

class BaseStateModifier(val statements:Seq[TemporalStatement], val actions:Seq[Action], val vars:Seq[Pair[String, VarRef]])
  extends StateModifier {

  val temporalConstraints = Nil

  def withStatements(addStatements:TemporalStatement*) = new BaseStateModifier(statements ++ addStatements, actions, vars)

  def withActions(addActions:Action*) = new BaseStateModifier(statements, actions ++ addActions, vars)

  def withVariables(addVars:Pair[String, VarRef]*) = new BaseStateModifier(statements, actions, vars ++ addVars)
}