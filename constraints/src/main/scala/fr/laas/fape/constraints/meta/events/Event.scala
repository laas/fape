package fr.laas.fape.constraints.meta.events

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable, VarWithDomain}

trait Event

sealed trait CSPEvent extends Event

case class NewConstraint(c: Constraint) extends CSPEvent

case class WatchConstraint(c: Constraint) extends CSPEvent

case class UnwatchConstraint(c: Constraint) extends CSPEvent

case class NewVariableEvent(v: IVar) extends CSPEvent

abstract class DomainChange(val variable: VarWithDomain) extends CSPEvent

case class DomainReduced(override val variable: VarWithDomain) extends DomainChange(variable)

case class DomainExtended(override val variable: VarWithDomain) extends DomainChange(variable)


case class Satisfied(constraint: Constraint) extends CSPEvent

trait WatchedSatisfactionUpdate extends CSPEvent {
  def constraint : Constraint
}
case class WatchedSatisfied(override val constraint: Constraint) extends WatchedSatisfactionUpdate
case class WatchedViolated(override val constraint: Constraint) extends WatchedSatisfactionUpdate