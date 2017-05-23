package fr.laas.fape.planning.events

import fr.laas.fape.anml.model
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.anml.model.concrete.statements.{Assignment, Persistence, Transition}
import fr.laas.fape.anml.model.{AnmlProblem, ParameterizedStateVariable, SymFunction}
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.ExtensionConstraint
import fr.laas.fape.constraints.meta.domains.ExtensionDomain
import fr.laas.fape.constraints.meta.events.{Event, InternalCSPEventHandler}
import fr.laas.fape.constraints.meta.stn.constraint.{Contingent, MinDelay}
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.planning.causality.CausalHandler
import fr.laas.fape.planning.causality.support.SupportByAction
import fr.laas.fape.planning.structures.{Change, Holds}
import fr.laas.fape.planning.types.{AnmlVarType, TypeHandler}
import fr.laas.fape.planning.variables.{FVar, InstanceVar, SVar, Var}

import scala.collection.JavaConverters._
import scala.collection.mutable

class PlanningHandler(_csp: CSP, base: Either[AnmlProblem, PlanningHandler]) extends InternalCSPEventHandler {

  implicit val csp = _csp
  def log = csp.log

  assert1(!csp.conf.enforceTpAfterStart, "Planner needs to be able some timepoints before the CSP's temporal origin.")

  val pb: AnmlProblem = base match {
    case Left(anmlProblem) => anmlProblem
    case Right(prev) => prev.pb
  }

  val variables: mutable.Map[VarRef, Var] = base match {
    case Right(prev) => prev.variables.clone()
    case Left(_) => mutable.Map()
  }

  val stateVariables: mutable.Map[ParameterizedStateVariable, SVar] = base match {
    case Right(prev) => prev.stateVariables.clone()
    case Left(_) => mutable.Map()
  }

  val functionVars: mutable.Map[SymFunction, FVar] = base match {
    case Left(_) => mutable.Map()
    case Right(prev) => prev.functionVars.clone()
  }

  val types: TypeHandler = base match {
    case Left(_) => new TypeHandler(pb)
    case Right(prev) => prev.types
  }

  val actions: mutable.ArrayBuffer[Action] = base match {
    case Right(prev) => prev.actions.clone()
    case Left(_) => mutable.ArrayBuffer()
  }

  val extensionDomains: mutable.Map[model.Function, ExtensionDomain] = base match {
    case Right(prev) => prev.extensionDomains.clone()
    case Left(_) => mutable.Map()
  }

  // last since causal handler typically need to access types and variables
  val subhandlers: mutable.ArrayBuffer[PlanningEventHandler] = base match {
    case Left(_) => mutable.ArrayBuffer(new CausalHandler(this))
    case Right(prev) => prev.subhandlers.map(_.clone(this))
  }

  def variable(v: VarRef): Var = v match {
    case v: InstanceRef =>
      variables.getOrElseUpdate(v, new InstanceVar(v, types.get(v.typ)))
    case _ =>
      variables.getOrElseUpdate(v, new Var(v, types.get(v.typ)))
  }

  def func(f: model.Function): FVar = f match {
    case f: SymFunction => functionVars.getOrElseUpdate(f, new FVar(f, types.functionType))
    case _ => throw new NotImplementedError()
  }

  def sv(psv: ParameterizedStateVariable): SVar =
    stateVariables.getOrElseUpdate(psv, new SVar(func(psv.func), psv.args.toList.map(variable(_)), psv))

  def getHandler[T](clazz: Class[T]) : T = subhandlers.filter(_.getClass == clazz).toList match {
    case Nil => throw new IllegalArgumentException("No handler of such type")
    case h :: Nil => h.asInstanceOf[T]
    case list => throw new IllegalArgumentException("Multiple handlers of such type")
  }

  def tp(tpRef: TPRef): Timepoint =
    if(tpRef == pb.start)
      csp.temporalOrigin
    else if(tpRef == pb.end)
      csp.temporalHorizon
    else
      csp.varStore.getTimepoint(tpRef)

  def insertChronicle(chronicle: Chronicle) {
    for(c <- chronicle.bindingConstraints.asScala)  c match {
      case c: VarInequalityConstraint =>
        csp.post(variable(c.leftVar) =!= variable(c.rightVar))
      case c: VarEqualityConstraint =>
        csp.post(variable(c.leftVar) === variable(c.rightVar))
      case c: AssignmentConstraint =>
        val func = c.sv.func
        assert1((c.sv.args.toList :+ c.variable).forall(variable(_).domain.isSingleton))
        val values = (c.sv.args.toList :+ c.variable).map(variable(_).domain.head)
        extensionDomains.getOrElseUpdate(func, new ExtensionDomain(values.size)).addTuple(values)
      case c: EqualityConstraint =>
        val variables = (c.sv.args.toList :+ c.variable).map(variable(_))
        csp.post(new ExtensionConstraint(variables, extensionDomains.getOrElseUpdate(c.sv.func, new ExtensionDomain(variables.size))))
      case x =>
        throw new NotImplementedError(s"Support for constraint $x is not implemented.")
    }
    for(c <- chronicle.temporalConstraints.asScala) c match {
      case c: MinDelayConstraint =>
        assert1(c.minDelay.isKnown, "Support for mixed constraint is not implemented yet.")
        csp.post(new MinDelay(tp(c.src), tp(c.dst), c.minDelay.get))
      case c: ContingentConstraint =>
        assert1(c.min.isKnown && c.max.isKnown, "Support for mixed constraints is not implemented yet")
        csp.post(new Contingent(tp(c.src), tp(c.dst), c.min.get, c.max.get))
    }
    for(s <- chronicle.logStatements.asScala) s match {
      case s: Persistence =>
        csp.post(tp(s.start) <= tp(s.end)) // todo: double check if those are needed
        csp.addEvent(PlanningStructureAdded(Holds(s, this)))
      case s: Transition =>
        csp.post(tp(s.start) < tp(s.end))
        csp.addEvent(PlanningStructureAdded(Change(s, this)))
        csp.addEvent(PlanningStructureAdded(Holds(s, this)))
      case s: Assignment =>
        csp.post(tp(s.start) < tp(s.end))
        csp.addEvent(PlanningStructureAdded(Change(s, this)))
    }
  }

  override def handleEvent(event: Event) {
    event match {
      case InitPlanner =>
        for(chronicle <- pb.chronicles.asScala)
          csp.addEvent(ChronicleAdded(chronicle))
      case ChronicleAdded(chronicle) =>
        insertChronicle(chronicle)
      case ActionInsertion(actionTemplate, support) =>
        val act = Factory.getStandaloneAction(pb, actionTemplate, RefCounter.getGlobalCounter)
        actions += act
        insertChronicle(act.chronicle)
        csp.post(csp.temporalOrigin <= tp(act.start))
        support match {
          case Some(supportVar) => csp.post(new SupportByAction(act, supportVar))
          case None =>
        }
      case e: PlanningStructureAdded =>
      case event: PlanningEvent =>
        throw new NotImplementedError(s"The event $event is not handle")
      case _ => // not concerned by this event
    }
    for(h <- subhandlers)
      h.handleEvent(event)
  }

  override def clone(newCSP: CSP): InternalCSPEventHandler =
    new PlanningHandler(newCSP, Right(this))

  def report: String = {
    val sb = new StringBuilder
    for(h <- subhandlers) {

      sb.append(s"\n--------- SubHandler report: $h -------\n")
      sb.append(h.report)
      sb.append("---------- Actions ---------\n")
      for(a <- actions.sortBy(a => tp(a.start).domain.lb)) {
        sb.append(s"[${tp(a.start).domain.lb}, ${tp(a.end).domain.lb}] ${a.name}(${a.args.asScala.map(p => p.label+"="+variable(p).dom).mkString(", ")})\n")
      }
    }
    sb.toString()
  }
}
