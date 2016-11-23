package fape.scenarios.morse

import java.util.logging.{Level, Logger}

import akka.actor.{ActorSystem, Props}
import fape.acting.CommandListener
import fape.acting.messages.GetProblemFromScene
import fape.core.planning.Plan
import fape.core.planning.planner.APlanner
import org.apache.commons.logging.LogFactory
import org.ros.scala.node._

object Main extends App {
  Logger.getGlobal.setLevel(Level.OFF)
  Logger.getLogger("ros").setLevel(Level.OFF)
  LogFactory.getLog("")
  Plan.makeDispatchable = true
  Plan.showChart = true
  APlanner.logging = false
  APlanner.debugging = false

  val system = ActorSystem("fape")

  val passer = system.actorOf(Props[ROSMessagePasser], name = "passer")
  Thread.sleep(500) // giving some time for ros node to initialize

  val manager = system.actorOf(Props[fape.acting.ActivityManager], name = "manager")

  //  val actor = system.actorOf(Props[Simulator], name = "actor")
  val actor = system.actorOf(Props[Dispatcher], name = "actor")
  val sensor = system.actorOf(Props[Observer], name = "observer")
  val commands = system.actorOf(Props[CommandListener], name = "commands")

  sensor ! GetProblemFromScene
}
