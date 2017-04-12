package fr.laas.fape.planning.events

import fr.laas.fape.constraints.meta.events.CSPEventHandler

trait PlanningEventHandler extends CSPEventHandler {

  def clone(newContext: PlanningHandler) : PlanningEventHandler

  def report : String
}
