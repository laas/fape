package fr.laas.fape.constraints.meta.logger

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.Variable

class ILogger {

  def stepIn() {}
  def stepOut() {}

  def eventDequeued(event: Event) {}

  def constraintPropagation(constraint: Constraint) {}

  def domainUpdate(variable: Variable, domain: Domain) {}

  def constraintPosted(constraint: Constraint) {}
}

class Logger extends ILogger {

  var offset = 0

  override def stepIn() { offset += 2 }
  override def stepOut() { offset -= 2 }

  private def printOffset() { print(" "*offset) }

  override def eventDequeued(event: Event): Unit = {
    printOffset()
    println(s"Event: $event")
  }

  override def constraintPropagation(constraint: Constraint): Unit = {
    printOffset()
    println(s"propagation: $constraint")
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
