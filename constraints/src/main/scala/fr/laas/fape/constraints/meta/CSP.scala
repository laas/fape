package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintSatisfaction, ReificationConstraint, ReversibleConstraint}
import fr.laas.fape.constraints.meta.domains.{BooleanDomain, Domain, EnumeratedDomain, IntervalDomain}
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.logger.{ILogger, Logger}
import fr.laas.fape.constraints.meta.stn.core.StnWithStructurals
import fr.laas.fape.constraints.meta.stn.events.STNEventHandler
import fr.laas.fape.constraints.meta.stn.variables.{TemporalDelay, Timepoint}
import fr.laas.fape.constraints.meta.variables._

import scala.collection.mutable

class CSP(toClone: Option[CSP] = None) {
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

  val varStore: VariableStore = toClone match {
    case None => new VariableStore(this)
    case Some(base) => base.varStore.clone(this)
  }

  val constraints: ConstraintStore = toClone match {
    case Some(base) => base.constraints.clone(this)
    case None => new ConstraintStore(this)
  }

  val stn: StnWithStructurals = toClone match {
    case None => new StnWithStructurals()
    case Some(base) => base.stn.clone()
  }
  val stnBridge: STNEventHandler = toClone match {
    case None => new STNEventHandler()(this)
    case Some(base) => base.stnBridge.clone(this)
  }
  // set the STN listener to the one already in the event handlers to get notified of temporal variable updates
  stn.setDistanceChangeListener(stnBridge)

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
    post(variable === value)
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
    constraints.handleEvent(event)
    event match {
      case NewConstraintEvent(c) =>
        c.propagate(event)
        if(c.isSatisfied)
            setSatisfied(c)
      case e@ DomainReduced(variable) =>
        for(c <- constraints.watching(variable)) {
          c.propagate(e)
          if(c.isSatisfied)
            setSatisfied(c)
        }
      case e@ DomainExtended(variable) =>
        for(c <- constraints.watching(variable)) {
          c.propagate(e)
          if(c.isSatisfied)
            setSatisfied(c)
        }
      case e: NewVariableEvent =>

      case x: Satisfied => // handled by constraint store
    }
    stnBridge.handleEvent(event)
    for(h <- eventHandlers)
      h.handleEvent(event)
    log.endEventHandling(event)
  }

  def post(constraint: Constraint) {
    log.constraintPosted(constraint)
    addEvent(NewConstraintEvent(constraint))
  }

  def reified(constraint: Constraint with ReversibleConstraint) : ReificationVariable = {
    if(!varStore.hasVariableForRef(constraint)) {
      val variable = varStore.getReificationVariable(constraint)
      domains.put(variable, new BooleanDomain(Set(false, true)))
      post(new ReificationConstraint(variable, constraint))
    }
    varStore.getReificationVariable(constraint)
  }

  def setSatisfied(constraint: Constraint)  {
    assert(constraint.isSatisfied)
    addEvent(Satisfied(constraint))
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
    str.append("%% ACTIVE CONSTRAINTS\n")
    for(c <- constraints.active.toSeq.sortBy(_.toString))
      str.append(s"$c  ${c.satisfaction}\n")

    str.append("%% SATISFIED CONSTRAINTS\n")
    for(c <- constraints.satisfied.toSeq.sortBy(_.toString))
      str.append(s"$c  ${c.satisfaction}\n")
    str.toString
  }
}
