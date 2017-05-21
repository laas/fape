package fr.laas.fape.constraints.meta.search.heuristics

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{Event, InternalCSPEventHandler}

/** Heuristics implementing a Depth First Search priority: priority is given to the deepest node and ties are broken by
  * prioritizing the earliest created CSP. */
class DFSHeuristic(csp: CSP) extends Heuristic {

  /** Gives the priority of a given CSP. Lower value means higher priority. */
  override def priority: Float = -csp.depth + csp.numberOfChildren.toFloat / 10000

  /** Invoked when a CSP is cloned, the new CSP will append the handler resulting from this method into its own handlers */
  override def clone(newCSP: CSP): InternalCSPEventHandler = new DFSHeuristic(newCSP)

  override def handleEvent(event: Event) {}
}
