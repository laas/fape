package fape.acting

import akka.actor.{FSM, Actor}
import akka.actor.Actor.Receive
import akka.event.Logging
import fape.Planning
import fape.actors.patterns.MessageLogger
import fape.core.planning.Planner
import fape.core.planning.planner.{PlannerFactory, TaskConditionPlanner, APlanner}
import PlanningActor._
import fape.core.planning.states.{Printer, State}
import planstack.anml.parser.ANMLFactory

import scala.concurrent.duration.FiniteDuration


object PlanningActor {
  def time() = System.currentTimeMillis()
  val repairTime = 10000
  sealed trait MState
  case object MainState extends MState
//  sealed trait Data


  sealed trait PlannerMessage
  object GetPlan extends PlannerMessage
  case class TryRepair(state: State, forHowLong: FiniteDuration, numPlanReq: Int) extends PlannerMessage
  case class TryReplan(state: State, forHowLong: FiniteDuration, numPlanReq: Int) extends PlannerMessage
  case object RepairFailed extends PlannerMessage
  case object ReplanFailed extends PlannerMessage
  case class PlanFound(state: fape.core.planning.states.State, numPlanReq: Int) extends PlannerMessage
}

class PlanningActor extends FSM[MState, Option[State]] with MessageLogger {

  val manager = context.actorSelection("..")

  startWith(MainState, None)

  when(MainState) {
    case Event(TryRepair(s, forHowLong, numPlanReq), _) =>
      val p = planner(s)
      val res = p.search(time() + forHowLong.toMillis)

      log.info(s"${p.planState}")
      if(res == null)
        manager ! RepairFailed
      else
        manager ! PlanFound(res, numPlanReq)
      stay()

    case Event(TryReplan(s, forHowLong, numPlanReq), _) =>
      val p = planner(s)
      val res = p.search(time() + forHowLong.toMillis)
      log.info(s"${p.planState}")
      if(res == null)
        manager ! ReplanFailed
      else
        manager ! PlanFound(res, numPlanReq)
      stay()

//    case Event(GetPlan, Some(p)) =>
//      val state = p.search(time + repairTime)
//      log.info(s"${p.planState}")
//      if(state == null)
//        manager ! NoPlanFound
//      else
//        manager ! PlanFound(state)
//      stay

  }


  private def planner(initialState: fape.core.planning.states.State) : APlanner =
    PlannerFactory.getPlannerFromInitialState(
      "taskcond",
      initialState,
      PlannerFactory.defaultPlanSelStrategies,
      PlannerFactory.defaultFlawSelStrategies)
}
