package fr.laas.fape.acting.actors.patterns

import akka.actor.{ActorRef, Actor}

import scala.concurrent._
import scala.concurrent.duration.FiniteDuration
import akka.pattern.{after, ask, pipe}

trait DelayedForwarder extends Actor {
  import context.dispatcher

  def forwardLater(delay: FiniteDuration, msg: Any, receiver: ActorRef)(implicit sender: ActorRef = Actor.noSender): Unit = {
    val f = after(delay, context.system.scheduler) {
      Future.successful(msg)
    }
    f.pipeTo(receiver)(sender)
  }

  def doFirstOf(nominal: Future[() => Unit], delay: FiniteDuration, fallback: () => Unit): Unit = {
    val fallbackFuture = after(delay, context.system.scheduler) {
      Future.successful(fallback)
    }
    doFirstOf(nominal, fallbackFuture)
  }

  def doFirstOf(wished: Future[() => Unit], fallback: Future[() => Unit]): Unit = {
    future {
      Future.firstCompletedOf(Seq(wished, fallback)).onSuccess { case func => func.apply() }
    }
  }
}
