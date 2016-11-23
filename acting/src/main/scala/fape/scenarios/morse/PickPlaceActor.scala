package fape.scenarios.morse


import adream_actions.Point
import akka.actor.{ActorRef, FSM}
import fape.acting._
import fape.acting.messages.{AAction, Success, Failed, Execute}
import fape.actors.patterns.{MessageLogger, DelayedForwarder}
import fape.core.execution.model.AtomicAction
import org.ros.scala.message.generated.adream_actions.SPickRequest
import org.ros.scala.message.generated.adream_actions.SPickResponse
import org.ros.scala.node._
import scala.collection.mutable
import scala.util.Random
import org.ros.scala.node.ServiceRequest
import org.ros.scala.message.generated.std_msgs.SString
import org.ros.scala.message.generated.adream_actions._
import scala.collection.JavaConverters._
import scala.concurrent.duration._

import PickPlaceActor._

object PickPlaceActor {
  def id() = Random.nextInt()

  sealed trait PickerData
  object NoAction extends PickerData
  case class ExecutingAction(act: AtomicAction, sender: ActorRef) extends PickerData
  case class Picking(item: String, reqID: Int) extends PickerData

  sealed trait State
  case object Blind extends State
  case object Idle extends State
  case object Active extends State
}

class PickPlaceActor(robot: String) extends FSM[State,PickerData] with DelayedForwarder with MessageLogger {
  import context.dispatcher

  private val pickService = Topics.pickService.format(robot)
  private val placeService = Topics.placeService.format(robot)

  startWith(Idle, NoAction)

  when(Idle) {
    case Event(Execute(act), NoAction) => act match {
      case AAction(ref, "Pick", List(`robot`, item, loc), _, _, _) =>
        Main.passer ! ServiceRequest(pickService, SPickRequest(item), ref.id)
        goto(Active) using ExecutingAction(act, sender())

      case AAction(ref, "Drop", List(`robot`, item, surf), _, _, _) =>
        Database.getSurfaceLocations match {
          case None =>
            log.warning("No information on surface locations in database. Delaying start of 1s.")
            forwardLater(1.seconds, Execute(act), self)(sender())
            stay()
          case Some(locs) =>
            val (x, y, z) = locs(surf)
            Main.passer ! ServiceRequest(placeService, SPlaceRequest(x.toFloat, y.toFloat, z.toFloat), ref.id)
            goto(Active) using ExecutingAction(act, sender())
        }
    }
    case Event(x, y) =>
      log.error(s"Unhandled: $x -- $y")
      stay()
   }

  when(Active) {
    case Event(ServiceResponse(serv, msg, id1), ExecutingAction(act, sender)) if id1 == act.id.id =>
      msg match {
        case SPickResponse(0) => sender ! Success(act)
        case SPickResponse(_) => sender ! Failed(act)
        case SPlaceResponse(0) => sender ! Success(act)
        case SPlaceResponse(_) => sender ! Failed(act)
      }
      log.error(s"Service Answer: $msg from $serv")
      goto(Idle) using NoAction

    case Event(ServiceFailure(serv, reqID), ExecutingAction(act, asker)) if reqID == act.id.id =>
      log.warning("Action failed: "+act)
      asker ! Failed(act)
      goto(Idle) using NoAction
  }

  whenUnhandled {
    case Event(e, x) =>
      log.error("Unhandled: " + e)
      stay()
    case x =>
      log.error("Received : " + x)
      stay()
  }

}