package fape.acting

import akka.actor.Actor
import akka.event.Logging
import scala.concurrent.duration._
import scala.util.Random


class PlanManager extends Actor {
  val log = Logging(context.system, this)

  val locs = List("office", "living", "entrance", "bedroom")


  override def receive = {
    case "start" => {
      println("Received start!")
      self ! "done"
    }
    case "done" => {
      context.actorSelection("../actor") ! ("dispatch", ("Move", "pr2", Random.shuffle(locs).head))
    }
    case "fail" =>
      log.info("SOmething went wrong, thinking now ....")
      import context.dispatcher
      context.system.scheduler.scheduleOnce(5 seconds, self, "done")
    case _ => println("Problem")
  }


}
