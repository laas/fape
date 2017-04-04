package fr.laas.fape.constraints.meta.events

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.variables.{IVar, Variable, WithDomain}

trait Event {

}

case class NewConstraintEvent(c: Constraint) extends Event

case class NewVariableEvent(v: IVar) extends Event

abstract class DomainChange(val variable: IVar with WithDomain) extends Event

case class DomainReduced(override val variable: IVar with WithDomain) extends DomainChange(variable)

case class DomainExtended(override val variable: IVar with WithDomain) extends DomainChange(variable)

