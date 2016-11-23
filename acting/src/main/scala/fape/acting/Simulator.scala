package fape.acting

import akka.actor.{ActorRef, Actor}
import akka.actor.Actor.Receive
import akka.event.Logging
import akka.event.slf4j.Logger
import fape.acting.messages.{Execute, Success, Failed}
import fape.actors.patterns.MessageLogger
import fape.core.execution.model.AtomicAction

import scala.concurrent.duration._
import scala.util.Random





class Simulator extends Actor with MessageLogger {
  val log = Logging(context.system, this)
  import context.dispatcher

  private var pending : List[(AtomicAction, ActorRef)] = Nil

  private val timeManager = context.actorSelection("akka://fape/user/manager/time-manager")

  override def receive: Receive = {
    case Execute(a) =>
      val s = sender()
      if(a.maxDuration > 1000 || a.name == "Point") { //TODO very ugly hack to distinguish controllable from uncontrollable
        pending = (a, s) :: pending
        timeManager ! SubscribeEndable(a)
      } else {
        val dur = a.minDuration + Random.nextInt(a.maxDuration - a.minDuration + 1)

        if (Random.nextInt(100) < 180)
          context.system.scheduler.scheduleOnce(dur.seconds, s, Success(a))
        else
          context.system.scheduler.scheduleOnce(dur.seconds, s, Failed(a))
      }

    case Endable(id) =>
      pending.find(p => p._1.id == id) match {
        case Some((a, s)) =>
          log.info("Endable: "+a)
          s ! Success(a)
        case None => log.error("No such action to end.")
      }
  }
}
