package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.TemporalStatement


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
  def statements : Iterable[TemporalStatement]

  /** Actions to be inserted in the plan */
  def actions : Iterable[Action]

  /** (Type, Name) of global variables to be declared */
  def vars : Iterable[Pair[String, String]]

}

class BaseStateModifier(val statements:Iterable[TemporalStatement], val actions:Iterable[Action], val vars:Iterable[Pair[String, String]])
  extends StateModifier {

  def withStatements(addStatements:TemporalStatement*) = new BaseStateModifier(statements ++ addStatements, actions, vars)

  def withActions(addActions:Action*) = new BaseStateModifier(statements, actions ++ addActions, vars)

  def withVariables(addVars:Pair[String, String]*) = new BaseStateModifier(statements, actions, vars ++ addVars)
}