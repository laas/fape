package fape.actors.patterns

import akka.actor.Actor
import akka.event.Logging
import fape.acting.ActivityManager.Tick

trait MessageLogger extends Actor {

  override def aroundReceive(receive: Actor.Receive, msg: Any): Unit = {
    msg match {
      case Tick =>
      case _ => Logging(context.system, this).info(s"From: [${sender().path}] --> $msg")
    }
    super.aroundReceive(receive, msg)
  }
}
