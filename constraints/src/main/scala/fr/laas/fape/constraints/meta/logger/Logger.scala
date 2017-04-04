package fr.laas.fape.constraints.meta.logger

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.Variable

class ILogger {

  def startEventHandling(event: Event) {}
  def endEventHandling(event: Event) {}

  def startConstraintPropagation(constraint: Constraint) {}
  def endConstraintPropagation(constraint: Constraint) {}

  def domainUpdate(variable: Variable, domain: Domain) {}

  def constraintPosted(constraint: Constraint) {}
}

class Logger extends ILogger {

  var offset = 0

  private def stepIn() { offset += 2 }
  private def stepOut() { offset -= 2 }
  private def printOffset() { print(" "*offset) }


  override def startEventHandling(event: Event): Unit = {
    printOffset()
    println(s"Event: $event")
    stepIn()
  }
  override def endEventHandling(event: Event): Unit = {
    stepOut()
  }

  override def startConstraintPropagation(constraint: Constraint) {
    printOffset()
    println(s"propagation: $constraint")
    stepIn()
  }
  override def endConstraintPropagation(constraint: Constraint): Unit = {
    stepOut()
  }

  override def domainUpdate(variable: Variable, domain: Domain): Unit = {
    printOffset()
    println(s"dom-update: $variable <- $domain")
  }

  override def constraintPosted(constraint: Constraint): Unit = {
    printOffset()
    println(s"new-constraint: $constraint")
  }
}
