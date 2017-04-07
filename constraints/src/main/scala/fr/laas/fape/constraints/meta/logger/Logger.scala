package fr.laas.fape.constraints.meta.logger

import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable, VarWithDomain}

class ILogger {

  def startEventHandling(event: Event) {}
  def endEventHandling(event: Event) {}

  def newEventPosted(event: Event) {}

  def startConstraintPropagation(constraint: Constraint) {}
  def endConstraintPropagation(constraint: Constraint) {}

  def domainUpdate(variable: VarWithDomain, domain: Domain) {}

  def constraintPosted(constraint: Constraint) {}

  def history: StringBuilder = new StringBuilder

  override def clone: ILogger = this
}

class Logger(previous: Option[Logger] = None) extends ILogger {

  val toSTDIO = false
  val recordHistory = true

  var offset = 0

  private def stepIn() { offset += 2 }
  private def stepOut() { offset -= 2 }
  private def printOffset() { print(" "*offset) }

  override val history: StringBuilder = previous match {
    case Some(base) => base.history.clone()
    case None => new StringBuilder
  }

  def print(msg: String) {
    if(recordHistory)
      history.append(msg)
    if(toSTDIO)
      Predef.print(msg)
  }

  def println(msg: String) {
    if(recordHistory) {
      history.append(msg)
      history.append("\n")
    }
    if(toSTDIO)
      Predef.println(msg)
  }

  override def startEventHandling(event: Event): Unit = {
    printOffset()
    println(s"Event: $event")
    stepIn()
  }
  override def endEventHandling(event: Event): Unit = {
    stepOut()
  }

  override def newEventPosted(event: Event) {
    printOffset()
    println("new-event: "+event)
  }

  override def startConstraintPropagation(constraint: Constraint) {
    printOffset()
    println(s"propagation: $constraint")
    stepIn()
  }
  override def endConstraintPropagation(constraint: Constraint): Unit = {
    stepOut()
  }

  override def domainUpdate(variable: VarWithDomain, domain: Domain): Unit = {
    printOffset()
    println(s"dom-update: $variable <- $domain")
  }

  override def constraintPosted(constraint: Constraint): Unit = {
    printOffset()
    println(s"new-constraint: $constraint")
  }

  override def clone: Logger = new Logger(Some(this))
}
