package fr.laas.fape.planning.events

import fr.laas.fape.anml.model.abs.AbstractAction
import fr.laas.fape.anml.model.concrete.Action
import fr.laas.fape.planning.causality.SupportVar

case class ActionInsertion(action: AbstractAction, supportFor: Option[SupportVar]) extends PlanningEvent
