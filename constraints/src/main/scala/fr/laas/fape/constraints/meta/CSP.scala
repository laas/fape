package fr.laas.fape.constraints.meta

import fr.laas.fape.anml.model.concrete.Variable
import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.events._

import scala.collection.mutable

class CSP {

  val domains = mutable.Map[Variable, Domain]()

  val events = mutable.ArrayBuffer[Event]()

  val constraints = mutable.ArrayBuffer[Constraint]()


  def dom(variable: Variable) : Domain = domains(variable)

  def updateDomain(variable: Variable, newDomain: Domain) = {
    if(dom(variable) != newDomain) {
      if(dom(variable).size < newDomain.size) {
        events += DomainReduced(variable, dom(variable) - newDomain)
      } else {
        assert(dom(variable).size > newDomain.size)
        events += DomainExtended(variable, newDomain - dom(variable))
      }
      domains(variable) = newDomain
    }
  }

  def handleEvent(event: Event) {
    event match {
      case NewConstraintEvent(constraint) =>
        constraint.propagate(event)
      case e: DomainReduced =>
        for(c <- constraints)
          c.propagate(e)
      case e: DomainExtended =>
        for(c <- constraints)
          c.propagate(e)
      case e: NewVariableEvent =>

    }
  }

  def post(constraint: Constraint) {
    constraints += constraint

    events += NewConstraintEvent(constraint)
  }
}
