package fape.acting

import akka.actor._
import akka.event.Logging
import fape.acting.PlanningActor.{TryReplan, TryRepair}
import fape.acting.messages.{ProblemFromScene, Failed, Success, Execute}
import fape.actors.patterns.MessageLogger
import fape.core.planning.{states, Plan}
import fape.core.planning.planner.APlanner.EPlanState
import fape.core.planning.planner.{APlanner, PlannerFactory}
import fape.core.planning.states.Printer
import fape.scenarios.morse.Observer.GetAllValues
import fape.util.ActionsChart
import planstack.anml.model.AnmlProblem
import planstack.anml.parser.ANMLFactory
import planstack.constraints.stnu.Controllability
import scala.concurrent.duration._
import scala.util.Random
import scala.collection.JavaConverters._
import scala.collection.JavaConverters._
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._

case class WholeProblem(anmlFile: String)

object ActivityManager {
  trait MState
  case object MIdle extends MState
  case object MDispatching extends MState
  case object MWaitingForPlan extends MState

  trait MData
  case object MNothing extends MData
  case class MPlanner(planner: APlanner) extends MData
  case class MPendingGoals(state: fape.core.planning.states.State, pendingGoals: List[String]) extends MData

  case class SetGoal(goal: String)

  object Tick
}
import ActivityManager._

class ActivityManager extends FSM[MState, MData] with MessageLogger {
  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1.minute) {
      case _: ActorKilledException      => Restart
      case e                            => Restart
    }

  val baseDomainFile = "/mnt/data/root-links/PreciseHome/these/morse/planning/anml/domain.anml.part"

  import context.dispatcher
  context.system.scheduler.schedule(0.seconds, 1.seconds, self, Tick)


  val planningActor = context.actorOf(Props[fape.acting.PlanningActor], name = "planner")
  val timeManager = context.actorOf(Props[TemporalConstraintsManager], name = "time-manager")

  val actionDispatcher = context.actorSelection("../actor")
  val observer = context.actorSelection("../observer")

  var currentPlanReq = 0

  startWith(MIdle, MNothing)

  when(MIdle) {
    case Event(ProblemFromScene(anml), MNothing) =>
      val pb = newProblem
      pb.extendWithAnmlFile(baseDomainFile)
      pb.extendWithAnmlText(anml)
      val state = new states.State(pb, Controllability.STN_CONSISTENCY)
      currentPlanReq += 1
      planningActor ! PlanningActor.TryReplan(state.cc(), 10.seconds, currentPlanReq)
      observer ! GetAllValues
      goto(MWaitingForPlan) using MPendingGoals(state, Nil)

    case Event(WholeProblem(anmlFile), MNothing) =>
      val pb = newProblem
      pb.extendWithAnmlFile(anmlFile)
      val state = new states.State(pb, Controllability.STN_CONSISTENCY)
      currentPlanReq += 1
      planningActor ! PlanningActor.TryReplan(state.cc(), 10.seconds, currentPlanReq)
      goto(MWaitingForPlan) using MPendingGoals(state, Nil)

    case Event(PlanningActor.PlanFound(state, numPlanReq), MNothing) =>
      goto(MDispatching) using MPendingGoals(state, Nil)
  }

  when(MWaitingForPlan) {
    case Event(PlanningActor.PlanFound(state, numPlanReq), MPendingGoals(_, _)) if numPlanReq == currentPlanReq =>
      log.info("Plan found:")
      log.info("=== Temporal databases === \n" + Printer.temporalDatabaseManager(state))
      log.info("\n=== Actions ===\n"+Printer.actionsInState(state))
      goto(MDispatching) using MPendingGoals(state, Nil)

    case Event(PlanningActor.RepairFailed, MPendingGoals(state, goals)) =>
      replan() // replan instead of repair so it does not try again
      stay()


    case Event(PlanningActor.ReplanFailed, MPendingGoals(s, goals)) =>
      log.error("Was not able to solve those goals: "+goals)
      goto(MIdle) using ActivityManager.MNothing


    case Event(SetGoal(goal), MPendingGoals(state, goals)) =>
      repair(additionalUpdate = Some(goal))
      goto(MWaitingForPlan) using MPendingGoals(state, goal :: goals)
  }

  when(MDispatching) {
    case Event(Tick, MPendingGoals(state, Nil)) => // there should be no pending goals while dispatching
      ActionsChart.displayState(state)
      if(!state.isConsistent) {
        repair()
        goto(MWaitingForPlan)
      } else {
        val plan = new Plan(state)
        for (a <- plan.getDispatchableActions(Time.now).asScala) {
          log.info(s"Dispatching $a [${a.minDuration}, ${a.maxDuration}]")
          actionDispatcher ! Execute(a)
          state.setActionExecuting(a.id, a.mStartTime)
        }
        timeManager ! plan

        if (state.isConsistent) {
          stay using MPendingGoals(state, Nil)
        } else {
          repair()
          goto(MWaitingForPlan)
        }
      }

    case Event(Success(a), MPendingGoals(state, Nil)) =>
      log.info(s"Succeeded: $a")
      state.setActionSuccess(a.id, Time.now)
      stay using MPendingGoals(state, Nil)

    case Event(Failed(a), MPendingGoals(state, Nil)) =>
      log.info(s"Failed: $a")
      state.setActionFailed(a.id, Time.now)
      repair()
      goto(MWaitingForPlan) using MPendingGoals(state, Nil)

    case Event(SetGoal(goal), MPendingGoals(state, Nil)) =>
      repair(additionalUpdate = Some(goal))
      goto(MWaitingForPlan) using MPendingGoals(state, goal :: Nil)
  }

  whenUnhandled {
    case Event(PlanningActor.PlanFound(state, numPlanReq), MPendingGoals(_, _)) if numPlanReq < currentPlanReq =>
      log.debug("Outdated plan found message.")
      stay()

    case Event(Tick, _) =>
      stay()

    case Event(e, d) =>
      log.error(s"Unhandled: $e from ${sender()}-- ($stateName, $stateData)")
      stay()
  }



  def repair(additionalUpdate: Option[String] = None): Unit = {
    val MPendingGoals(state, baseUpdates) = this.stateData
    val updates = additionalUpdate match {
      case Some(update) => update :: baseUpdates
      case None => baseUpdates
    }
    val baseState = stateWithUpdates(state, updates)

    log.debug("TRYING REPAIR ON:")
    log.debug("=== Temporal databases === \n" + Printer.temporalDatabaseManager(baseState))
    log.debug("\n=== Actions ===\n"+Printer.actionsInState(baseState))

    currentPlanReq += 1
    planningActor ! Kill
    planningActor ! TryRepair(baseState, 1.seconds, currentPlanReq)
  }

  def replan(): Unit = {
    val MPendingGoals(state, updates) = this.stateData
    val baseState = stateWithUpdates(state.getCleanState, updates)

    log.debug("TRYING REPLAN ON:")
    log.debug("=== Temporal databases === \n" + Printer.temporalDatabaseManager(baseState))
    log.debug("\n=== Actions ===\n"+Printer.actionsInState(baseState))

    planningActor ! Kill
    currentPlanReq += 1
    planningActor ! TryReplan(baseState, 10.seconds, currentPlanReq)
  }

  def stateWithUpdates(baseState: states.State, updates: Seq[String]) = {
    val s = baseState.cc()

    for(update <- updates) {
      val chronicle = baseState.pb.getChronicleFromAnmlText(update)
      s.applyChronicle(chronicle)
    }
    s
  }

  private def newProblem = new AnmlProblem(usesActionConditions = true)
}