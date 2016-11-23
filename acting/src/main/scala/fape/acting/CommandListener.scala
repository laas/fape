package fape.acting

import akka.actor.Actor
import akka.actor.Actor.Receive
import akka.event.Logging
import fape.acting.ActivityManager.SetGoal
import org.ros.scala.message.generated.std_msgs.SString
import org.ros.scala.node.{ROSMsg, Subscribe}

class CommandListener extends Actor {
  val log = Logging(context.system, this)

  private val setGoalTopic = "fape/setGoal"
  private val passer = context.actorSelection("../passer")
  private val manager = context.actorSelection("../manager")

  passer ! Subscribe(setGoalTopic, "std_msgs/String")

  override def receive: Receive = {
    case ROSMsg(`setGoalTopic`, SString(anmlGoal)) =>
      log.info(s"Received: "+ROSMsg(`setGoalTopic`, SString(anmlGoal))+" from "+sender())
      manager ! SetGoal(anmlGoal)
  }
}
