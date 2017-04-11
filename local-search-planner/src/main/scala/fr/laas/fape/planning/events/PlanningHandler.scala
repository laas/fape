package fr.laas.fape.planning.events

import fr.laas.fape.anml.model.AnmlProblem
import fr.laas.fape.anml.model.concrete.{VarEqualityConstraint, VarInequalityConstraint, VarRef}
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{CSPEventHandler, Event}
import fr.laas.fape.constraints.meta.types.TypedVariable
import fr.laas.fape.planning.types.{AnmlVarType, TypeHandler}

import scala.collection.JavaConverters._

class PlanningHandler(_csp: CSP, base: Either[AnmlProblem, PlanningHandler]) extends CSPEventHandler {

  implicit val csp = _csp
  def log = csp.log

  val pb: AnmlProblem = base match {
    case Left(pb) => pb
    case Right(prev) => prev.pb
  }

  var types: TypeHandler = base match {
    case Left(pb) => new TypeHandler(pb)
    case Right(prev) => prev.types
  }

  def variable(v: VarRef): TypedVariable[String] =
    csp.variable(v, types.get(v.typ))


  override def handleEvent(event: Event) {
    event match {
      case InitPlanner =>
        println("")
        for(instance <- pb.instances.allInstances.asScala.map(name => pb.instances.referenceOf(name)))
          csp.post(variable(instance) === instance.instance)
        for(chronicle <- pb.chronicles.asScala)
          csp.addEvent(ChronicleAdded(chronicle))
      case ChronicleAdded(chronicle) =>
        log.info(s"Added: $chronicle")
        for(c <- chronicle.bindingConstraints.asScala) {
          c match {
            case c: VarInequalityConstraint =>
              csp.post(variable(c.leftVar) =!= variable(c.rightVar))
            case c: VarEqualityConstraint =>
              csp.post(variable(c.leftVar) === variable(c.rightVar))
          }
        }
      case event: PlanningEvent =>
        throw new NotImplementedError(s"The event $event is not handle")
      case _ => // not concerned by this event
    }
  }

  override def clone(newCSP: CSP): CSPEventHandler = new PlanningHandler(newCSP, Right(this))
}
