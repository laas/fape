package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.TemporalStatement

trait StateModifier {

  def statements : Iterable[TemporalStatement]
  def actions : Iterable[Action]

}

class BaseStateModifier(val statements:Iterable[TemporalStatement], val actions:Iterable[Action])
  extends StateModifier {

  def withStatements(addStatements:TemporalStatement*) = new BaseStateModifier(statements ++ addStatements, actions)

  def withActions(addActions:Action*) = new BaseStateModifier(statements, actions ++ addActions)
}