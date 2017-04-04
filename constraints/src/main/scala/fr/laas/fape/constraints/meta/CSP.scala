package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintSatisfaction, ReversibleConstraint, ReificationConstraint}
import fr.laas.fape.constraints.meta.domains.{BooleanDomain, Domain, EnumeratedDomain, IntervalDomain}
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.logger.{ILogger, Logger}
import fr.laas.fape.constraints.meta.stn.core.StnWithStructurals
import fr.laas.fape.constraints.meta.stn.events.STNEventHandler
import fr.laas.fape.constraints.meta.stn.variables.{TemporalDelay, Timepoint}
import fr.laas.fape.constraints.meta.variables.{BooleanVariable, IVar, Variable, VariableStore}

import scala.collection.mutable

class CSP(toClone: Option[CSP] = None) {
  import ConstraintSatisfaction._
  implicit private val csp = this

  val domains : mutable.Map[Variable, Domain] = toClone match {
    case Some(base) => base.domains.clone()
    case None => mutable.Map()
  }

  val events: mutable.Queue[Event] = toClone match {
    case None => mutable.Queue()
    case Some(base) => base.events.clone()
  }

  val eventHandlers: mutable.ArrayBuffer[CSPEventHandler] = toClone match {
    case None => mutable.ArrayBuffer()
    case Some(base) => base.eventHandlers.map(handler => handler.clone(this))
  }

  val constraints: mutable.ArrayBuffer[Constraint] = toClone match {
    case None => mutable.ArrayBuffer[Constraint]()
    case Some(base) => base.constraints.clone()
  }

  val varStore: VariableStore = toClone match {
    case None => new VariableStore(this)
    case Some(base) => base.varStore.clone(this)
  }

  val stn: StnWithStructurals = toClone match {
    case None =>
      eventHandlers += new STNEventHandler
      new StnWithStructurals()
    case Some(base) =>
      base.stn.clone()
  }
  // set the STN listener to the one already in the event handlers to get notified of temporal variable updates
  stn.setDistanceChangeListener(eventHandlers.find(_.isInstanceOf[STNEventHandler]).get.asInstanceOf[STNEventHandler])

  val temporalOrigin = varStore.getTimepoint(":start:")
  val temporalHorizon = varStore.getTimepoint(":end:")

  override def clone : CSP = new CSP(Some(this))

  final val log : ILogger = new Logger

  def dom(variable: Variable) : Domain = domains(variable)

  def dom(tp: Timepoint) : IntervalDomain =
    new IntervalDomain(stn.getEarliestTime(tp), stn.getLatestTime(tp))

  def dom(d: TemporalDelay) : IntervalDomain =
    new IntervalDomain(stn.getMinDelay(d.from, d.to), stn.getMaxDelay(d.from, d.to))

  def bind(variable: Variable, value: Int) {
    updateDomain(variable, new EnumeratedDomain(Set(value)))
  }

  def updateDomain(variable: Variable, newDomain: Domain) {
    log.domainUpdate(variable, newDomain)
    if(dom(variable).size > newDomain.size) {
      events += DomainReduced(variable)
      domains(variable) = newDomain
    } else if(dom(variable).size < newDomain.size) {
      events += DomainExtended(variable)
      domains(variable) = newDomain
    }
  }

  def propagate(): Unit = {
    while(events.nonEmpty) {
      val e = events.dequeue()
      handleEvent(e)
    }
  }

  def handleEvent(event: Event) {
    log.startEventHandling(event)
    event match {
      case NewConstraintEvent(constraint) =>
        constraint.propagate(event)
      case e: DomainReduced =>
        for(c <- constraints if c.variables.contains(e.variable)) {
          c.propagate(e)
        }
      case e: DomainExtended =>
        for(c <- constraints if c.variables.contains(e.variable)) {
          c.propagate(e)
        }
      case e: NewVariableEvent =>
    }
    for(h <- eventHandlers)
      h.handleEvent(event)
    log.endEventHandling(event)
  }

  def post(constraint: Constraint) {
    log.constraintPosted(constraint)
    constraints += constraint
    events += NewConstraintEvent(constraint)
  }

  def reified(constraint: Constraint with ReversibleConstraint) : BooleanVariable = {
    if(!varStore.hasVariableForRef(constraint)) {
      val variable = varStore.getBooleanVariable(Some(constraint))
      varStore.setVariableForRef(constraint, variable)
      domains.put(variable, new BooleanDomain(Set(false, true)))
      post(new ReificationConstraint(variable, constraint))
    }
    varStore.getVariableForRef(constraint).asInstanceOf[BooleanVariable]
  }

  def setSatisfied(constraint: Constraint): Unit = {
    assert(constraint.satisfaction == SATISFIED)
  }

  def addVariable(variable: Any, domainValues: Set[Int]) : Variable = {
    val v = variable match {
      case v: Variable => v
      case v: IVar => throw new RuntimeException("Only Variables can be given a domain")
      case ref => varStore.getVariableForRef(ref)
    }
    assert(!hasVariable(v))
    val domain = new EnumeratedDomain(domainValues)
    addVariable(v, domain)
    v
  }

  def addVariable(variable: Variable, domain: Domain): Unit = {
    assert(!hasVariable(variable))
    domains.put(variable, domain)
    variableAdded(variable)
  }

  /** Records an event notifying of the variable addition + some sanity checks */
  def variableAdded(variable: IVar): Unit = {
    variable match {
      case v: Variable => assert(domains.contains(v), "Variable has no domain")
      case _ =>
    }
    addEvent(NewVariableEvent(variable))
  }

  def addEvent(event: Event): Unit = {
    events += event
  }

  def hasVariable(variable: Variable) : Boolean = domains.contains(variable)

  def nextVarId() = varStore.getNextVariableId()


  def report : String = {
    val str = new StringBuilder
    for((v, d) <- domains.toSeq.sortBy(_._1.toString)) {
      str.append(s"$v = $d\n")
    }
    for(c <- constraints.sortBy(_.toString))
      str.append(s"$c  ${c.satisfaction}\n")
    str.toString
  }
}
