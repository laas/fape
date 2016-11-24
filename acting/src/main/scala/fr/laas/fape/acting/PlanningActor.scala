package fr.laas.fape.acting

import akka.actor.FSM
import fr.laas.fape.acting.PlanningActor._
import fr.laas.fape.acting.actors.patterns.MessageLogger
import fr.laas.fape.planning.Planning
import fr.laas.fape.planning.core.planning.planner.Planner.EPlanState
import fr.laas.fape.planning.core.planning.planner.{Planner, PlanningOptions}
import fr.laas.fape.planning.core.planning.states.State
import fr.laas.fape.planning.exceptions.PlanningInterruptedException
import fr.laas.fape.planning.util.TinyLogger

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext.Implicits.global

object PlanningActor {
  def time() = System.currentTimeMillis()
  val repairTime = 10000
  sealed trait MState
  case object MIdle extends MState
  case object MPlanning extends MState
//  sealed trait Data


  sealed trait PlannerMessage
  object GetPlan extends PlannerMessage
  case class GetPlan(state: State, forHowLong: FiniteDuration, reqID: Int)
  case class TryRepair(state: State, forHowLong: FiniteDuration, numPlanReq: Int) extends PlannerMessage
  case class TryReplan(state: State, forHowLong: FiniteDuration, numPlanReq: Int) extends PlannerMessage
  case object RepairFailed extends PlannerMessage
  case object ReplanFailed extends PlannerMessage
  case class PlanFound(state: State, numPlanReq: Int) extends PlannerMessage
  case class NoPlanExists(reqID: Int) extends PlannerMessage
  case class PlanningTimedOut(reqID: Int) extends PlannerMessage
}

class PlanningActor extends FSM[MState, Option[Planner]] with MessageLogger {

  val manager = context.actorSelection("..")

  startWith(MIdle, None)

  when(MIdle) {
    case Event(GetPlan(initPlan, duration, reqID), _) =>
      val options = new PlanningOptions()
      val planner = new Planner(initPlan, options)
      launchPlanningProcess(planner, duration, reqID)
      goto(MPlanning) using Some(planner)
  }
  when(MPlanning) {
    case Event(GetPlan(initPlan, duration, reqID), Some(previousPlanner)) =>
      previousPlanner.stopPlanning = true
      val planner = new Planner(initPlan, new PlanningOptions())
      launchPlanningProcess(planner, duration, reqID)
      stay() using Some(planner)
  }

  def launchPlanningProcess(planner: Planner, duration: FiniteDuration, reqID: Int): Unit = {
    Future {
      try {
        val solution = planner.search(time() + duration.toMillis)
        if (solution != null) {
          manager ! PlanFound(solution, reqID)
          log.info("Got Plan")
        } else if(planner.planState == EPlanState.TIMEOUT) {
          manager ! PlanningTimedOut(reqID)
        } else {
          manager ! NoPlanExists(reqID)
        }
      } catch {
        case x: PlanningInterruptedException =>
          log.info(s"Planning interrupted ($reqID)")
      }
    }
  }


  private def planner(initialState: fr.laas.fape.planning.core.planning.states.State) : Planner =
//    PlannerFactory.getPlannerFromInitialState(
//      "taskcond",
//      initialState,
//      PlannerFactory.defaultPlanSelStrategies,
//      PlannerFactory.defaultFlawSelStrategies)
    null
}
