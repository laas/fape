package fr.laas.fape.constraints.meta.stn.events

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, IEventHandler, NewConstraintEvent, NewVariableEvent}
import fr.laas.fape.constraints.meta.stn.constraint.TemporalConstraint
import fr.laas.fape.constraints.meta.stn.core.StnWithStructurals
import fr.laas.fape.constraints.meta.stn.variables.{TemporalDelay, Timepoint}

class STNEventHandler(val stn: StnWithStructurals, implicit val csp: CSP) extends IEventHandler {
  require(csp.stn == stn)


  override def handleEvent(event: Event): Unit = event match {
    case NewVariableEvent(tp: Timepoint) =>
      if(tp == csp.temporalOrigin)
        stn.recordTimePointAsStart(tp)
      else if(tp == csp.temporalHorizon)
        stn.recordTimePointAsEnd(tp)
      else {
        stn.recordTimePoint(tp)
        stn.enforceBefore(csp.temporalOrigin, tp)
        stn.enforceBefore(tp, csp.temporalHorizon)
      }
    case NewVariableEvent(delay: TemporalDelay) =>
      // TODO: add to watch list
    case NewConstraintEvent(c: TemporalConstraint) =>
      // nothing to do, handled in propagation directly
    case NewConstraintEvent(c) =>
    // not a pure temporal constraint, check if there is any temporal variables in it
      for(v <- c.variables) {
        v match {
          case tp: Timepoint => // TODO: add to watch list
          case d: TemporalDelay => // TODO: add to watch list
          case _ => // ignore constraint
        }
      }

    case _ =>
  }
}
