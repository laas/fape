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

  def info(str: => String) { println(str) }
  def warning(str: => String) { println(str) }
  def error(str: => String) { println(str) }

  def history: StringBuilder = new StringBuilder

  override def clone: ILogger = this
}

class Logger(previous: Option[Logger] = None) extends ILogger {

  val toSTDIO = false
  val recordHistory = false

  var offset = 0

  private def stepIn() { offset += 2 }
  private def stepOut() { offset -= 2 }
  private def printOffset() { print(" "*offset) }

  override val history: StringBuilder = previous match {
    case Some(base) => base.history.clone()
    case None => new StringBuilder
  }

  def print(msg: => String, params: Any*) {
    if(recordHistory)
      history.append(msg)
    if(toSTDIO)
      Predef.print(msg)
  }

  def println(msg: => String, params: Any*) {
    if(recordHistory) {
      history.append(msg.format(params: _*))
      history.append("\n")
    }
    if(toSTDIO)
      Predef.println(msg.format(params: _*))
  }

  override def info(str: => String) { printOffset(); println("INFO: %s", str) }
  override def warning(str: => String) { printOffset(); println("WARNING: %s", str) }
  override def error(str: => String) { printOffset(); println("ERROR: %s", str) }

  override def startEventHandling(event: Event): Unit = {
    printOffset()
    println("Event: %s", event)
    stepIn()
  }
  override def endEventHandling(event: Event): Unit = {
    stepOut()
  }

  override def newEventPosted(event: Event) {
    printOffset()
    println("new-event: %s", event)
  }

  override def startConstraintPropagation(constraint: Constraint) {
    printOffset()
    println("propagation: %s", constraint)
    stepIn()
  }
  override def endConstraintPropagation(constraint: Constraint): Unit = {
    stepOut()
  }

  override def domainUpdate(variable: VarWithDomain, domain: Domain): Unit = {
    printOffset()
    println("dom-update: %s <- %s", variable, domain)
  }

  override def constraintPosted(constraint: Constraint): Unit = {
    printOffset()
    println("new-constraint: %s", constraint)
  }

  override def clone: Logger = new Logger(Some(this))
}
