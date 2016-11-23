package fape.acting

import akka.actor.FSM
import fape.acting._
import org.ros.scala.node._
import scala.util.Random
import org.ros.scala.node.ServiceRequest
import org.ros.scala.message.generated.std_msgs.SString
import org.ros.scala.message.generated.adream_actions.SPickRequest


object PickPlaceFSM {
  def id() = Random.nextInt()

  sealed trait PickerData
  object IdleData extends PickerData
  case class Picking(item: String, reqID: Int) extends PickerData
}

import PickPlaceFSM._

case class DoPick(robot: String, item: String)

class PickPlaceFSM(robot: String) extends FSM[State,PickerData] {
  private val pickService = s"$robot/picker/pick"
  val placeTopic = "%s/nav_status".format(robot)

  val passer = context.actorSelection("../passer")


  startWith(Idle, NoAction)

  when(Idle) {
    case Event(DoPick(`robot`, item), NoAction) =>
      val req = id()
      passer ! ServiceRequest("pr2/picker/pick", SPickRequest(item), req)
      goto(Active) using Picking(item, req)
    case Event("start", y) =>
      passer ! ServiceRequest("pr2/picker/place", SString("MyCornflakes"), id())
      stay()
    case Event(x, y) =>
      log.info(x.toString)
      stay()
   }

  when(Active) {
    case Event(ServiceResponse(serv, msg, id1), Picking(item, id2)) if id1 == id2 =>
      log.info(s"Service Answer: $msg from $serv")
      goto(Idle) using NoAction
  }

  whenUnhandled {
    case Event(e, x) =>
      log.error("Something is wrong with this message: " + e)
      stay()
    case x =>
      log.error("Received : " + x)
      stay()
  }

}