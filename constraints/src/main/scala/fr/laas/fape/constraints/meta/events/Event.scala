package fr.laas.fape.constraints.meta.events

import fr.laas.fape.anml.model.concrete.Variable
import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain

trait Event {

}

case class NewConstraintEvent(c: Constraint) extends Event

case class NewVariableEvent(v: Variable) extends Event

trait DomainEvent extends Event

case class DomainReduced(variable: Variable, removedValues: Domain) extends DomainEvent

case class DomainExtended(variable: Variable, addedValues: Domain) extends DomainEvent

