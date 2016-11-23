package fape.acting

import akka.actor.{FSM, ActorRef, Actor, Props}
import akka.event.Logging
import org.ros.node.topic.Publisher
import scala.collection.immutable
import scala.concurrent.duration._
import org.ros.scala.node._
import org.ros.scala.message.generated.std_msgs.SString

// states
sealed trait State
case object Idle extends State
case object Active extends State

sealed trait Data
case object NoGoal extends Data
final case class CurrentGoal(goal: String) extends Data

class NavFSM(robot: String) extends FSM[State,Data] {
  val navStatusTopic = "%s/nav_status".format(robot)
  val navGoalTopic   = "/%s/nav_goal".format(robot)

  val passer = context.actorSelection("../../passer")
  passer ! Subscribe(navStatusTopic, "std_msgs/String")

  startWith(Idle, NoGoal)

  when(Idle) {
    case Event(("Move", `robot`, x: String), NoGoal) => {
      passer ! Publish(navGoalTopic, SString(x))
      goto(Active) using CurrentGoal(x)
    }
  }

  when(Active) {
    case Event(ROSMsg(`navStatusTopic`, data: std_msgs.String), CurrentGoal(goal)) => {
      data.getData match {
        case "done" =>
          context.parent ! ("Success", ("Move", robot, goal))
        case "error" =>
          log.warning("Error in nav_goal exec of " + goal)
          context.parent ! ("Failure", ("Move", "pr2", goal))
        case "timeout" =>
          log.info("nav_goal " + goal + "timed out.")
          context.parent ! ("Failure", ("Move", "pr2", goal))
        case x => log.info(x)
      }
      goto(Idle) using NoGoal
    }
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
/*
class NavActor(robot: String) extends Actor {
  val log = Logging(context.system, this)

  val passer = context.actorSelection("../../passer")

  passer ! org.ros.scala.node.Subscribe("/pr2/nav_status")

  import context.dispatcher

  var currentGoal = ""

  def receive = {
    case ("Move", `robot`, x: String) => {
      log.info("Received move request for %s to %s".format(robot, x))
      currentGoal = x
      passer ! org.ros.scala.node.Publish("/pr2/nav_goal", x)
//      context.system.scheduler.scheduleOnce(15000 milliseconds, self, ("Success", x))
    }
    case ("Success", x) => context.parent ! ("Success", ("Move", robot, x))
    case org.ros.scala.node.ROSMsg(topic, data: std_msgs.String) =>
      println("Messageclass: " + data.getClass.getName)
      println("ros msg: "+data)
      println("data: -" + data.getData + "-")
      data.getData match {
        case "done" => context.parent ! ("Success", ("Move", robot, currentGoal))
        case "error" => log.warning("Error")
        case "timeout" => log.info("timeout")
        case _ =>
      }
      log.info("Receveid:  " + data.getData + "  on  "+topic)
      currentGoal = ""
    case x =>
      println("Received unknown: " + x)
  }
}
*/
