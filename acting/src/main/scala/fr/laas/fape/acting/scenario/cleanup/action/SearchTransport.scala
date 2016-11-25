package fr.laas.fape.acting.scenario.cleanup.action

import akka.actor.FSM
import fr.laas.fape.acting.{Clock, Utils}
import fr.laas.fape.acting.actors.patterns.MessageLogger
import fr.laas.fape.acting.messages.{ExecutionRequest, Failed, TimepointActive, TimepointExecuted}
import fr.laas.fape.acting.scenario.cleanup.action.NavigateTo.MFailure
import fr.laas.fape.ros.ROSUtils
import fr.laas.fape.ros.action.{GoToPick, MoveBaseClient, MoveBlind}
import fr.laas.fape.ros.database.Database
import fr.laas.fape.ros.exception.ActionFailure
import fr.laas.fape.ros.message.MessageFactory
import fr.laas.fape.ros.request.GTPUpdate
import gtp_ros_msg.Pt

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object NavigateTo {

  case class MData(req: Option[ExecutionRequest], targetPose: Option[Pt])
  case class MFailure(req: ExecutionRequest)
}

import NavigateTo.MData

class NavigateTo(robot: String)  extends FSM[String,MData] with MessageLogger {

  startWith("idle", MData(None,None))

  when("idle") {
    case Event(exe:ExecutionRequest, _) =>
      lookUpTargetPose(exe)
      goto("wait-start-and-pose") using MData(Some(exe), None)
  }

  when("wait-start-and-pose") {
    case Event(pose:Pt, MData(x, None)) =>
      goto("wait-for-start") using MData(x, Some(pose))

    case Event(TimepointActive(tp), MData(Some(req), None)) =>
      assert(tp == req.action.context.getTimepoint("nav"))
      goto("wait-for-pose")
  }

  when("wait-for-start") {
    case Event(TimepointActive(tp), MData(Some(req), Some(target))) =>
      assert(tp == req.action.context.getTimepoint("nav"))
      goto("navigating")
  }

  when("wait-for-pose") {
    case Event(targetPose:Pt, MData(Some(req), None)) =>
      goto("navigating") using MData(Some(req), Some(targetPose))
  }

  onTransition {
    case _ -> "navigating" =>
      assert(nextStateData.req.nonEmpty && nextStateData.targetPose.nonEmpty)
      val (req, pose) = (nextStateData.req.get, nextStateData.targetPose.get)
      val startNavTP = req.action.context.getTimepoint("nav")
      req.caller ! TimepointExecuted(startNavTP, Clock.time())
      Future {
//        MoveBaseClient.sendGoTo(pose.getX, pose.getY, pose.getZ) //TODO
        Thread.sleep(3)
        self ! "target-reached"
      }
  }

  when("navigating") {
    case Event("target-reached", MData(Some(exe), _)) =>
      exe.caller ! TimepointExecuted(exe.action.end, Clock.time())
      goto("idle") using MData(None,None)

    case Event(req:ExecutionRequest, MData(Some(previous), _)) =>
      log.info("Received a new request while navigating, stopping previous activity")
      MoveBlind.cancelAllGoals()
      MoveBaseClient.cancelAllGoals()
      lookUpTargetPose(req)
      goto("wait-start-and-pose") using MData(Some(req), None)
  }

  whenUnhandled {
    case Event(MFailure(req), MData(Some(exe), _)) if req == exe =>
      exe.caller ! Failed(exe)
      log.error("Failed to executed current request")
      goto("idle") using MData(None,None)

    case Event(MFailure(req), MData(Some(exe), _)) if req != exe =>
      log.info("Got Failure from cancelled request")
      stay()
  }

  def lookUpTargetPose(req: ExecutionRequest): Unit = {
    assert(req.name == "NavigateTo")
    val container = req.plan.taskNet.getContainingAction(req.action)
    val (obj,dist) =
      if(container.name.contains("GoPick"))
        (Utils.asString(container.args.get(1), req.plan), GoToPick.PICK_DISTANCE)
      else if(container.name.contains("GoPlace"))
        (Utils.asString(container.args.get(2), req.plan), GoToPick.PLACE_DISTANCE)
      else
        throw new ActionFailure("Navigate to is no part of another action")
    Future {
      try {
        val bot :: table :: _ = req.parameters
        val angle = GoToPick.getFeasibleApproach(bot, obj, dist)
        val targetObjectPose = MessageFactory.getXYYawFromPose(Database.getPoseOf(obj))
        if(angle == null)
          throw new ActionFailure("GoToPick: no possible approaches to "+obj)

        val preManipPose = angle.getApproachPose(targetObjectPose, dist+GoToPick.ENGAGE_DIST)
        log.info("Got pose")
        self ! preManipPose
      } catch {
        case e:ActionFailure =>
          log.error("Action failure: "+e.getMessage)
          e.printStackTrace()
          self ! MFailure(req)
      }
    }
  }

}
