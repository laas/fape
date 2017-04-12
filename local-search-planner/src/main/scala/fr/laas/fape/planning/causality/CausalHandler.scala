package fr.laas.fape.planning.causality

import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.stn.variables.Timepoint
import fr.laas.fape.planning.events.{PlanningEventHandler, PlanningHandler, PlanningStructureAdded}
import fr.laas.fape.planning.structures.{CausalStruct, Change, Holds}

import scala.collection.mutable

class CausalHandler(context: PlanningHandler, base: Option[CausalHandler] = None) extends PlanningEventHandler {

  implicit val csp = context.csp

  val changes: mutable.ArrayBuffer[Change] = base match {
    case Some(prev) => prev.changes.clone()
    case None => mutable.ArrayBuffer()
  }

  val holds:  mutable.ArrayBuffer[Holds] = base match {
    case Some(prev) => prev.holds.clone()
    case None => mutable.ArrayBuffer()
  }

  def report : String = {
    def time(tp: Timepoint) : String = s"$tp:${tp.domain}"
    val sb = new StringBuilder
    sb.append("%% Changes\n")
    for(c <- changes) {
      sb.append(s"${c.sv}:=${c.value} -- change:]${time(c.changing.start)}, ${time(c.changing.end)}[ -- persist:[${time(c.persists.start)}, ${time(c.persists.end)}]\n")
    }
    sb.append("%% Holds\n")
    for(c <- holds) {
      sb.append(s"${c.sv}==${c.value} -- persist:[${time(c.persists.start)}, ${time(c.persists.end)}]\n")
    }
    sb.toString()
  }

  override def handleEvent(event: Event) {
    event match {
      case PlanningStructureAdded(s: Holds) =>
        for(h <- holds)
          csp.post(Threat(s, h))
        for(c <- changes)
          csp.post(Threat(c, s))
        csp.post(new SupportConstraint(s))
        holds += s
      case PlanningStructureAdded(s: Change) =>
        csp.post(s.persists.start <= s.persists.end)
        for(h <- holds)
          csp.post(Threat(s, h))
        for(c <- changes)
          csp.post(Threat(c, s))
        changes += s
      case PlanningStructureAdded(_: CausalStruct) => throw new NotImplementedError()

      case _ => // not a causal structure, ignore
    }
  }

  def clone(newContext: PlanningHandler) : CausalHandler = new CausalHandler(newContext, Some(this))
}
