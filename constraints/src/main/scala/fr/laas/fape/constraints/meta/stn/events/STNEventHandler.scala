package fr.laas.fape.constraints.meta.stn.events

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.stn.constraint.TemporalConstraint
import fr.laas.fape.constraints.meta.stn.core.{IDistanceChangeListener, StnWithStructurals}
import fr.laas.fape.constraints.meta.stn.variables.{TemporalDelay, Timepoint}
import fr.laas.fape.constraints.meta.util.Assertion._

class STNEventHandler(implicit val csp: CSP)
  extends CSPEventHandler with IDistanceChangeListener {

  def stn = csp.stn

  override def handleEvent(event: Event): Unit = {
    event match {
      case NewVariableEvent(tp: Timepoint) =>
        // record any new timepoint in the STN, with special case for Origin and horizon
        if(tp == csp.temporalOrigin) {
          stn.recordTimePointAsStart(tp)
        } else if(tp == csp.temporalHorizon) {
          stn.recordTimePointAsEnd(tp)
        } else {
          stn.recordTimePoint(tp)
          stn.enforceBefore(csp.temporalOrigin, tp)
          stn.enforceBefore(tp, csp.temporalHorizon)
        }
      case NewConstraint(c: TemporalConstraint) =>
      // nothing to do, handled in propagation directly
      case NewConstraint(c) =>
        // not a pure temporal constraint, check if there is any temporal variables to watch in it
        for((tp1, tp2) <- watches(c))
          stn.addWatchedDistance(tp1, tp2)
      case NewWatchedConstraint(c) =>
        // not a pure temporal constraint, check if there is any temporal variables to watch in it
        for((tp1, tp2) <- watches(c))
          stn.addWatchedDistance(tp1, tp2)
      case Satisfied(c: TemporalConstraint) => // no watches were recorded by this constraint
      case Satisfied(c) =>
      // not a pure temporal constraint, check if there is any temporal variables to watch in it
      //      for(v <- c.variables) v match {
      //        case tp: Timepoint =>
      //          stn.removeWatchedDistance(csp.temporalOrigin, tp)
      //        case d: TemporalDelay =>
      //          stn.removeWatchedDistance(d.from, d.to)
      //        case _ => // ignore variable
      //      }
      case e: WatchedSatisfactionUpdate =>
      //       not a pure temporal constraint, check if there is any temporal variables to watch in it
      //      for(v <- e.constraint.variables) v match {
      //        case tp: Timepoint =>
      //          stn.removeWatchedDistance(csp.temporalOrigin, tp)
      //        case d: TemporalDelay =>
      //          stn.removeWatchedDistance(d.from, d.to)
      //        case _ => // ignore variable
      //      }
      case _ =>
    }
    watchesSanityChecks()
  }

  /** Returns all delay that a gien constraint should be monitoring. */
  private def watches(c: Constraint) : Iterable[(Timepoint, Timepoint)] = {
    c.variables.collect {
      case tp: Timepoint => (csp.temporalOrigin, tp)
      case d: TemporalDelay => (d.from, d.to)
    }
  }

  /** Checks that all delay monitored by actived and watched constraints are notified to the STN*/
  private def watchesSanityChecks() {
    if(csp.events.isEmpty) { // there might be non recorded event watches as long as the event queue is not empty
      assert3(csp.constraints.active.flatMap(watches(_)).forall(p => stn.isWatched(p._1, p._2)),
        "A distance of an active constraint is not recorded in the STN")
      assert3(csp.constraints.watched.flatMap(watches(_)).forall(p => stn.isWatched(p._1, p._2)),
        "A distance of a watched constraint is not recorded in the STN")
    }
  }

  /** Handles the notification from the STN that the distance between two timepoints has been updated. */
  override def distanceUpdated(tp1: Timepoint, tp2: Timepoint) {
    csp.addEvent(DomainReduced(csp.varStore.getDelayVariable(tp1, tp2)))
    if(tp1 == csp.temporalOrigin)
      csp.addEvent(DomainReduced(tp2))
  }

  override def clone(newCSP: CSP): STNEventHandler = new STNEventHandler()(newCSP)
}
