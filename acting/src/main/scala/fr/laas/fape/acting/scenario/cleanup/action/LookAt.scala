package fr.laas.fape.acting.scenario.cleanup.action

import akka.actor.FSM
import fr.laas.fape.acting.Clock
import fr.laas.fape.acting.actors.patterns.MessageLogger
import fr.laas.fape.acting.messages.{ExecutionRequest, TimepointExecuted}
import fr.laas.fape.ros.ROSUtils
import fr.laas.fape.ros.action.{GoToPick, MoveBaseClient, MoveBlind}
import fr.laas.fape.ros.database.Database
import fr.laas.fape.ros.exception.ActionFailure
import fr.laas.fape.ros.message.MessageFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LookAt(robot: String)  extends FSM[String,Option[ExecutionRequest]] with MessageLogger {


  startWith("idle", None)

  when("idle") {
    case Event(exe:ExecutionRequest, _) =>
      assert(exe.name == "LookAt")
      Future {
        try {
          val bot :: target :: _ = exe.parameters
          if (ROSUtils.dist(Database.getPoseOf(bot), Database.getPoseOf(target)) > 2.1) {
            log.error("To far from target")
            self ! "failure"
          } else {
            fr.laas.fape.ros.action.LootAt.lookAt(target)
            self ! "success"
          }
        } catch {
          case e:ActionFailure =>
            log.error("Action failure")
            e.printStackTrace()
            self ! "failure"
        }

      }

      goto("active") using Some(exe)
  }

  when("active") {
    case Event("success", Some(exe)) =>
      exe.caller ! TimepointExecuted(exe.action.end, Clock.time())
      goto("idle") using None

    case Event("failure", Some(exe)) =>
      log.error("FAILURE")
      goto("idle") using None

    case Event(req:ExecutionRequest, Some(previous)) =>
      MoveBlind.cancelAllGoals()
      MoveBaseClient.cancelAllGoals()
      goto("idle") using Some(req)
  }


}
