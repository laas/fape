package fr.laas.fape.constraints.meta.events

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.variables.{IVar, Variable}

trait Event {

}

case class NewConstraintEvent(c: Constraint) extends Event

case class NewVariableEvent(v: IVar) extends Event

abstract class DomainChange(val variable: Variable) extends Event

case class DomainReduced(override val variable: Variable, removedValues: Domain) extends DomainChange(variable)

case class DomainExtended(override val variable: Variable, addedValues: Domain) extends DomainChange(variable)

