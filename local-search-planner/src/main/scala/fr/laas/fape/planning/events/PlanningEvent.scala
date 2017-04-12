package fr.laas.fape.planning.events

import fr.laas.fape.anml.model.concrete.Chronicle
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.planning.structures.PStruct

trait PlanningEvent extends Event

object InitPlanner extends PlanningEvent

case class ChronicleAdded(chronicle: Chronicle) extends PlanningEvent

case class PlanningStructureAdded(struct: PStruct) extends PlanningEvent