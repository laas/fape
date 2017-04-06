package fr.laas.fape.constraints.meta.events

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable, VarWithDomain}

trait Event {

}

case class NewConstraint(c: Constraint) extends Event

case class NewWatchedConstraint(c: Constraint) extends Event

case class NewVariableEvent(v: IVar) extends Event

abstract class DomainChange(val variable: VarWithDomain) extends Event

case class DomainReduced(override val variable: VarWithDomain) extends DomainChange(variable)

case class DomainExtended(override val variable: VarWithDomain) extends DomainChange(variable)


case class Satisfied(constraint: Constraint) extends Event

trait WatchedSatisfactionUpdate extends Event {
  def constraint : Constraint
}
case class WatchedSatisfied(override val constraint: Constraint) extends WatchedSatisfactionUpdate
case class WatchedViolated(override val constraint: Constraint) extends WatchedSatisfactionUpdate