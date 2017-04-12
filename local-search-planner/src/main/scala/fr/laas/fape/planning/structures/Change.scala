package fr.laas.fape.planning.structures

import fr.laas.fape.anml.model.concrete.statements.LogStatement
import fr.laas.fape.constraints.meta.stn.variables.TemporalInterval
import fr.laas.fape.planning.events.PlanningHandler
import fr.laas.fape.planning.variables.{SVar, Var}

class Change(val sv: SVar, val value: Var, val changing: TemporalInterval, val persists: TemporalInterval, val ref: LogStatement)
  extends CausalStruct {
  assert(ref.isChange)

  override def toString = s"$sv := $value"
}

object Change {

  def apply(statement: LogStatement, p: PlanningHandler) : Change = {
    require(statement.isChange)
    new Change(
      p.sv(statement.sv),
      p.variable(statement.endValue),
      new TemporalInterval(p.tp(statement.start), p.tp(statement.end)),
      new TemporalInterval(p.tp(statement.end), p.csp.varStore.getTimepoint()),
      statement)
  }

}
