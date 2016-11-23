package fape.acting

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import fape.acting.drawing.SVG
import fape.actors.patterns.MessageLogger
import fape.core.execution.model.AtomicAction
import fape.core.planning.Plan
import fape.scenarios.brackets.Main
import planstack.anml.model.concrete.ActRef

case class Endable(act: ActRef)
case class SubscribeEndable(act: AtomicAction)

class TemporalConstraintsManager extends Actor with MessageLogger {

  var endableActionsSubscribers = Map[ActRef, List[ActorRef]]()

  override def receive: Receive = {
    case p:Plan =>
      for(act <- endableActionsSubscribers.keys ; if p.isEndable(act)) {
        for(subscriber <- endableActionsSubscribers(act))
          subscriber ! Endable(act)
        endableActionsSubscribers = endableActionsSubscribers.updated(act, Nil)
      }
      SVG.printSvgToFile(p.getState, s"${Main.outDir}plan-${Time.now}.svg")

    case SubscribeEndable(act) =>
      endableActionsSubscribers = endableActionsSubscribers.updated(act.id, sender() :: endableActionsSubscribers.getOrElse(act.id, Nil))
  }
}
