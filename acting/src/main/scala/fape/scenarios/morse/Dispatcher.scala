package fape.scenarios.morse

import akka.actor.{Actor, Props}
import akka.event.Logging
import fape.acting.messages.{AAction, Execute}
import fape.actors.patterns.MessageLogger

class Dispatcher extends Actor with MessageLogger {
  val log = Logging(context.system, this)

  val nav = context.actorOf(Props(classOf[NavActor], "pr2"), name = "nav_pr2")
  val picker = context.actorOf(Props(classOf[PickPlaceActor], "pr2"), name = "picker_pr2")
  val searcher = context.actorOf(Props(classOf[SearchActor], "pr2"), name = "searcher_pr2")

  def receive = {
    case exe: Execute =>
      println(s"Received execution Request: $exe")
      exe match {
      case Execute(AAction(_, "Move", List(robot, from, to), start, minDur, maxDur)) =>
        println(s"Received Move \\o/ $robot $from $to")
        nav forward exe

      case Execute(AAction(_, "Pick", List(robot, item, place), _, _, _)) =>
        println(s"Received Pick \\o/: ${exe.a}")
        picker forward exe

      case Execute(AAction(_, "Drop", List(robot, item, place), _, _, _)) =>
        println(s"Received Drop \\o/: ${exe.a}")
        picker forward exe

      case Execute(AAction(_, "Search", List(robot, item), _, _, _)) =>
        searcher forward exe

      case x => log.error(s"Unhandled: $x")
    }
    case x => log.error(s"Unhandled: $x")
  }

}
