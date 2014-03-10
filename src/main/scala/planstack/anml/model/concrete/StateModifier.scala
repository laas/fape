package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.TemporalStatement

trait StateModifier {

  def statements : Iterable[TemporalStatement]
  def actions : Iterable[Action]

}
