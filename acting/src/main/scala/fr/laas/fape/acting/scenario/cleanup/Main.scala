package fape.scenarios.morse

import java.io.File
import java.util.logging.{Level, Logger}

import akka.actor.{ActorSystem, Props}
import fr.laas.fape.acting.{ActivityManager, Utils}
import fr.laas.fape.anml.model.AnmlProblem
import fr.laas.fape.anml.model.concrete.Chronicle
import fr.laas.fape.anml.model.concrete.statements.Persistence
import fr.laas.fape.constraints.stnu.Controllability
import fr.laas.fape.planning.Planning
import fr.laas.fape.planning.core.planning.planner.{Planner, PlanningOptions}
import fr.laas.fape.planning.core.planning.states.modification.ChronicleInsertion
import fr.laas.fape.planning.core.planning.states.{Printer, State}
import fr.laas.fape.planning.util.TinyLogger
import fr.laas.fape.ros.GTP
import fr.laas.fape.ros.action._
import fr.laas.fape.ros.database.Database
import fr.laas.fape.ros.sensing.CourseCorrection
import org.apache.commons.logging.LogFactory


object Main extends App {
  Utils.setProblem("/home/abitmonn/working/robot.anml")
  Planning.quiet = false
  Planning.verbose = true
  TinyLogger.logging = true

  Logger.getGlobal.setLevel(Level.OFF)
  Logger.getLogger("ros").setLevel(Level.OFF)
  LogFactory.getLog("")
//  Plan.makeDispatchable = true
//  Plan.showChart = true
//  APlanner.logging = false
//  APlanner.debugging = false

  // start database
  Database.initialize()

  // initialize all ros clients (mainly to be done of annoying execution messages)
  MoveBaseClient.getInstance()
  MoveBlind.getInstance()
  MoveTorsoActionServer.getInstance()
  MoveLeftArm.getInstance()
  MoveRightArm.getInstance()
  GripperOperator.getInstance()
  MoveArmToQ.getInstance()
  LootAt.getInstance()
  GTP.getInstance()
  
  CourseCorrection.spin()
  MoveTorsoActionServer.moveTorso(0.3)
//  MoveBaseClient.sendGoTo(1,1,0) // necessary since Move3D stocks a lot of stuff in (0,0)

  val system = ActorSystem("fape")

  val manager = system.actorOf(Props[ActivityManager], name = "manager")

  //  val actor = system.actorOf(Props[Simulator], name = "actor")
  val actor = system.actorOf(Props[Dispatcher], name = "actor")
//  val sensor = system.actorOf(Props[Observer], name = "observer")
//  val commands = system.actorOf(Props[CommandListener], name = "commands")

//  sensor ! GetProblemFromScene




  val pplan = new State(Utils.getProblem, Controllability.PSEUDO_CONTROLLABILITY)
  val goal = Utils.buildTask("GoLook", List("PR2_ROBOT", "TABLE")) //Utils.buildGoal("Table.observed", List("TABLE_0"), "true", 3)
  pplan.apply(goal, false)
  val goal2 = Utils.buildTask("GoLook", List("PR2_ROBOT", "TABLE_0")) // Utils.buildGoal("Table.observed", List("TABLE"), "true", 3)
  pplan.apply(goal2, false)
//  manager ! goal
//  manager ! goal2
  val pickTask = Utils.buildTask("GoPick", List("PR2_ROBOT","GREY_TAPE"))
  manager ! pickTask
//  val options = new PlanningOptions()
//  options.displaySearch = true

//  val planner = new Planner(pplan, options)
//  val sol = planner.search(System.currentTimeMillis()+5000)
//  if(sol == null)
//    println("Error")
//  else
//    println(Printer.actionsInState(sol))
//  System.exit(0)
}
