package fr.laas.fape.constraints.meta.search.heuristics

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, InternalCSPEventHandler}

/** Defines a heuristic to be attached to the CSP. The heuristic will receive all internal events of the CSP. */
trait Heuristic extends InternalCSPEventHandler {


  /** Gives the priority of a given CSP. Lower value means higher priority. */
  def priority: Float
}


