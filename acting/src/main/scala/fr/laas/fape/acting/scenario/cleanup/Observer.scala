package fape.scenarios.morse

import akka.actor.{Actor, Props}
import akka.event.Logging
import fr.laas.fape.acting.Clock
import fr.laas.fape.acting.actors.patterns.MessageLogger
import fr.laas.fape.acting.messages._
import fr.laas.fape.acting.scenario.cleanup.NavActor
import fr.laas.fape.acting.scenario.cleanup.action.{ApproachToLook, LookAt, NavigateTo}
import fr.laas.fape.anml.model.concrete.Action

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class Dispatcher extends Actor with MessageLogger {
  val log = Logging(context.system, this)

  val actors = Map(
    "ApproachToLook" -> context.actorOf(Props(classOf[ApproachToLook], "pr2"), name = "approach-to-look"),
    "LookAt" -> context.actorOf(Props(classOf[LookAt], "pr2"), name = "look-at"),
    "NavigateTo" -> context.actorOf(Props(classOf[NavigateTo], "pr2"), name = "navigate-to")
  )

  def receive = {
    case e:ExecutionRequest =>
      actors.get(e.name) match {
        case Some(actor) =>
          actor forward e
        case None =>
          log.error(s"No actor for ${e.name}, simulating execution")
          val s = sender()
          Future {
            Thread.sleep(3000)
            s ! TimepointExecuted(e.action.end, Clock.time())
          }
      }
    case (actionName: String, TimepointActive(tp)) =>
      actors.get(actionName) match {
        case Some(actor) => actor ! TimepointActive(tp)
        case None =>
          log.error(s"No actor for $actionName, simulating execution of active timepoint")
          val s = sender()
          Future {
            Thread.sleep(3000)
            s ! TimepointExecuted(tp, Clock.time())
          }
      }
    case x => log.error(s"Unhandled: $x")
  }

}
