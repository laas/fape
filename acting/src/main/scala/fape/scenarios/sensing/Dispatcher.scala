package fape.scenarios.sensing

import akka.actor.{Props, Actor}
import akka.actor.Actor.Receive
import fape.acting.ActivityManager.SetGoal
import fape.acting._
import fape.acting.messages.{AAction, Success, Execute}
import fape.actors.patterns.{MessageLogger, DelayedForwarder}
import scala.concurrent.duration._

case class Sensed(anml: String)

class Dispatcher extends Actor with MessageLogger with DelayedForwarder {

  val simulator = context.actorOf(Props[Simulator], "simulator"
  )
  override def receive: Receive = {
    case Execute(act) => act match {
      case AAction(_, "LookFor", _, _, _, _) =>
        forwardLater(3.seconds, SetGoal("[%d] i.loc := l2;".format(Time.now + 3)), sender())
        forwardLater(4.seconds, Success(act), sender())
      case _ =>
        simulator forward Execute(act)

    }
  }
}
