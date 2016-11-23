package fape.scenarios.morse

import akka.actor.{ActorRef, FSM}
import fape.acting.messages.{AAction, Execute, Success, Failed}
import fape.core.execution.model.AtomicAction
import org.ros.scala.message.generated.std_msgs.SString
import org.ros.scala.node._


object NavActor {
  // states
  sealed trait State
  case object Idle extends State
  case object Active extends State

  sealed trait Data
  case object NoGoal extends Data
  final case class CurrentGoal(act: AtomicAction, sender: ActorRef) extends Data
}
import fape.scenarios.morse.NavActor._

class NavActor(robot: String) extends FSM[State,Data] {
  val navStatusTopic = Topics.navStatus.format(robot)
  val navGoalTopic   = Topics.navGoal.format(robot)

  val passer = context.actorSelection("../../passer")
  passer ! AddPublisher(navGoalTopic, "std_msgs/String")
  passer ! Subscribe(navStatusTopic, "std_msgs/String")

  startWith(Idle, NoGoal)

  when(Idle) {
    case Event(Execute(aa), NoGoal) => aa match {
      case AAction(_, name, List(`robot`, from, to), start, minDur, maxDur) =>
        passer ! Publish(navGoalTopic, SString(to))
        goto(Active) using CurrentGoal(aa, sender())
    }
  }

  when(Active) {
    case Event(ROSMsg(`navStatusTopic`, data: std_msgs.String), CurrentGoal(act, sender)) => {
      data.getData match {
        case "done" =>
          sender ! Success(act) //("Success", ("Move", robot, goal))
        case "error" =>
          log.warning(s"Error in nav_goal exec of $act")
          sender ! Failed(act)
        case "timeout" =>
          log.warning(s"nav_goal $act timed out.")
          sender ! Failed(act)
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