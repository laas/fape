package fape.scenarios.brackets

import akka.actor.{Props, ActorSystem}
import fape.acting.PlanningActor.{PlanFound, TryReplan}
import fape.acting.{Time, Simulator, WholeProblem}
import fape.core.planning.Plan
import fape.core.planning.planner.{PlannerFactory, APlanner}
import fape.core.planning.states.State
import planstack.anml.model.AnmlProblem
import planstack.constraints.stnu.Controllability
import scala.concurrent.duration._

object Main extends App {

  APlanner.logging = false
  Plan.makeDispatchable = true
  Plan.showChart = false

  val outDir = "out/svg-final/"
  val dom = "/mnt/data/root-links/PreciseHome/these/code/working/fape-planning/domains/saphari-brackets/brackets.anml"
//  val outDir = "out/svg-init/"
//  val dom = "/mnt/data/root-links/PreciseHome/these/code/working/fape-planning/domains/saphari-brackets/brackets-init.anml"
  val goal1 = "GlueAttach(operator1, PR2, as1, glue2);"
  val goal2 = "ProcessSurface(operator2, PR3, as2, glue);"
//  val goal1 = ""
//  val goal2 = ""

  val system = ActorSystem("fape")
  val manager = system.actorOf(Props[fape.acting.ActivityManager], name = "manager")

  val actor = system.actorOf(Props[Simulator], name = "actor")

  val pb = new AnmlProblem(false)
  pb.extendWithAnmlFile(dom)
  val st = new State(pb, Controllability.STN_CONSISTENCY)
  val ch = pb.getChronicleFromAnmlText(goal1)
  st.applyChronicle(ch)
  val planner = PlannerFactory.getPlanner("htn", st)
  val sol1 = planner.search(Time.millis + 20000)

  if(sol1 != null) {
    val ch2 = pb.getChronicleFromAnmlText(goal2)
    sol1.applyChronicle(ch2)
    val p = PlannerFactory.getPlanner("htn", sol1)
    val sol2 = p.search(Time.millis + 20000)
    manager ! PlanFound(sol2, 1)
  } else {
    println("No plan")
    sys.exit(1)
  }


//  val actor = system.actorOf(Props[Dispatcher], name = "actor")
  //val commands = system.actorOf(Props[CommandListener], name = "commands")



}