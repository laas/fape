package fr.laas.fape.planning.events

import fr.laas.fape.anml.model.concrete.Chronicle
import fr.laas.fape.constraints.meta.events.Event

trait PlanningEvent extends Event

object InitPlanner extends PlanningEvent

case class ChronicleAdded(chronicle: Chronicle) extends PlanningEvent