package fape.scenarios.sensing

import java.util.logging.{Level, Logger}

import akka.actor.{Props, ActorSystem}
import fape.acting.WholeProblem
import fape.core.planning.Plan
import fape.core.planning.planner.APlanner
import org.apache.commons.logging.LogFactory

object MainSensing extends App {

  APlanner.logging = false
  Plan.makeDispatchable = true
  Plan.showChart = false

  val system = ActorSystem("fape")
  import system.dispatcher

  val manager = system.actorOf(Props[fape.acting.ActivityManager], name = "manager")

  //  val actor = system.actorOf(Props[Simulator], name = "actor")
  val actor = system.actorOf(Props[Dispatcher], name = "actor")
  //val commands = system.actorOf(Props[CommandListener], name = "commands")

  manager ! WholeProblem("/mnt/data/root-links/PreciseHome/these/code/working/fape-planning/domains/sensing.anml")

}
