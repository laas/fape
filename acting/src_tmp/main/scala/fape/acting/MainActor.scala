package fape.acting

import akka.actor.{Props, Actor}
import akka.event.Logging
import fape.scenarios.morse.NavActor

class MainActor extends Actor {
  val log = Logging(context.system, this)

  val nav = context.actorOf(Props(classOf[NavActor], "pr2"), name = "nav_pr2")

  def receive = {
    case ("dispatch", ("Move", "pr2", x)) => {
      log.info("Received dispatch ...")
      nav ! ("Move", "pr2", x)
    }
    case ("Success", ("Move", "pr2", x)) => {
      log.info("well done !")
      context.actorSelection("../PlanManager") ! "done"
    }
    case ("Failure", ("Move", "pr2", x)) => {
      log.info("ARRRRH!!!")
      context.actorSelection("../PlanManager") ! "fail"
    }
    case _ => println("error")
  }

}
