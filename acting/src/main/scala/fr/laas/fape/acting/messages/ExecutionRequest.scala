package fr.laas.fape.acting.messages

import akka.actor.{Actor, ActorRef}
import fr.laas.fape.anml.model.concrete.Action
import fr.laas.fape.planning.core.planning.states.State

import scala.collection.JavaConverters._

class ExecutionRequest(val action: Action, val plan: State, val caller: ActorRef) {

  def name = action.name
  def parameters = action.args.asScala.map(a => plan.valuesOf(a).get(0).instance).toList
}
