package fr.laas.fape.planning.events

import fr.laas.fape.anml.model
import fr.laas.fape.anml.model.concrete._
import fr.laas.fape.anml.model.concrete.statements.{Assignment, Persistence, Transition}
import fr.laas.fape.anml.model.{AnmlProblem, ParameterizedStateVariable, SymFunction}
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, InternalCSPEventHandler}
import fr.laas.fape.constraints.meta.stn.constraint.{Contingent, MinDelay}
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.constraints.meta.types.{TypedVariable, TypedVariableWithInitialDomain}
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.meta.variables.VariableSeq
import fr.laas.fape.planning.causality.CausalHandler
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
    case Left(pb) => pb
    case Right(prev) => prev.pb
  }

  val subhandlers: mutable.ArrayBuffer[PlanningEventHandler] = base match {
    case Left(_) => mutable.ArrayBuffer(new CausalHandler(this))
    case Right(prev) => prev.subhandlers.map(_.clone(this))
  }

  val types: TypeHandler = base match {
    case Left(pb) => new TypeHandler(pb)
    case Right(prev) => prev.types
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

  def tp(tpRef: TPRef): Timepoint =
    if(tpRef == pb.start)
      csp.temporalOrigin
    else if(tpRef == pb.end)
      csp.temporalHorizon
    else
      csp.varStore.getTimepoint(tpRef)

  override def handleEvent(event: Event) {
    event match {
      case InitPlanner =>
        for(instance <- pb.instances.allInstances.asScala.map(name => pb.instances.referenceOf(name)))
          csp.post(variable(instance) === instance.instance)
        for(chronicle <- pb.chronicles.asScala)
          csp.addEvent(ChronicleAdded(chronicle))
      case ChronicleAdded(chronicle) =>
        for(c <- chronicle.bindingConstraints.asScala)  c match {
          case c: VarInequalityConstraint =>
            csp.post(variable(c.leftVar) =!= variable(c.rightVar))
          case c: VarEqualityConstraint =>
            csp.post(variable(c.leftVar) === variable(c.rightVar))
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
            csp.addEvent(PlanningStructureAdded(Holds(s, this)))
          case s: Transition =>
            csp.addEvent(PlanningStructureAdded(Change(s, this)))
            csp.addEvent(PlanningStructureAdded(Holds(s, this)))
          case s: Assignment =>
            csp.addEvent(PlanningStructureAdded(Change(s, this)))
        }
      case e: PlanningStructureAdded =>
      case event: PlanningEvent =>
        throw new NotImplementedError(s"The event $event is not handle")
      case _ => // not concerned by this event
    }
    for(h <- subhandlers)
      h.handleEvent(event)
  }

  override def clone(newCSP: CSP): InternalCSPEventHandler = new PlanningHandler(newCSP, Right(this))

  def report: String = {
    val sb = new StringBuilder
    for(h <- subhandlers) {
      sb.append(s"\n--------- SubHandler report: $h -------")
      sb.append(h.report)
    }
    sb.toString()
  }
}
