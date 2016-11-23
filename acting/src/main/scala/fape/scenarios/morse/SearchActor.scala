package fape.scenarios.morse

import akka.actor.ActorRef
import fape.acting.ActivityManager.SetGoal
import fape.acting.Time
import fape.acting.messages.{Failed, Success, AAction, Execute}
import fape.core.execution.model.AtomicAction
import org.ros.scala.message.generated.std_msgs.SString
import org.ros.scala.node.{ROSMsg, Publish, Subscribe, AddPublisher}

import SearchActor._

object SearchActor {
  // states
  sealed trait State
  case object Idle extends State
  case object Active extends State

  trait Data
  case class SearchingData(act: AtomicAction, asker: ActorRef, visited: Set[String], item:String) extends Data
  case object IdleData extends Data
}

import akka.actor.FSM

class SearchActor(robot: String) extends FSM[State, Data] {
  val navStatusTopic = Topics.navStatus.format(robot)
  val navGoalTopic = Topics.navGoal.format(robot)

  val passer = context.actorSelection("../../passer")
  passer ! AddPublisher(navGoalTopic, "std_msgs/String")
  passer ! Subscribe(navStatusTopic, "std_msgs/String")

  startWith(Idle, IdleData)

  when(Idle) {
    case Event(Execute(aa), _) => aa match {
      case AAction(_, name, List(`robot`, item), start, minDur, maxDur) =>
        val visited : Set[String] = Database.lastLocation(robot) match {
          case Some(ts) =>
            Set(ts.loc)
          case None =>
            log.error("No known location for "+robot)
            Set()
        }
        getNextToVisit(visited) match {
          case Some(next) =>
            passer ! Publish(navGoalTopic, SString(next))
            goto(Active) using SearchingData(aa, sender(), visited, item)
          case None =>
            log.error("Problem, no next place to visit.")
            stay()
        }

    }
  }

  when(Active) {
    case Event(ROSMsg(`navStatusTopic`, data: std_msgs.String), SearchingData(act, asker, visited, item)) => {
      val robotLoc = Database.lastLocation(robot) match {
        case Some(TimeStampedLocation(t, loc)) => loc
        case None =>
          log.error("Error: robot location is not known.")
          throw new RuntimeException("Error: robot location is not known.")
      }
      data.getData match {
        case "error" =>
          log.warning(s"Error in nav_goal exec of $act")
          sender ! Failed(act)
          goto(Idle) using IdleData
        case "timeout" =>
          log.warning(s"nav_goal $act timed out.")
          sender ! Failed(act)
          goto(Idle) using IdleData
        case "done" =>
          // we are where we want, was the item found yet?
          Database.lastLocation(item) match {
            case Some(TimeStampedLocation(t, loc)) =>
              log.info(s"Found item $item at $loc")
              val anml =
                s"""
                  |[${Time.now}] $robot.location := $robotLoc;
                  |[${Time.now}] $item.location := $loc;
                """.stripMargin
              asker ! SetGoal(anml)
              asker ! Success(act)
              goto(Idle) using IdleData

            case None =>
              log.info("Not found, keep searching.")
              val newVisited = visited + robotLoc
              getNextToVisit(newVisited) match {
                case Some(next) =>
                  passer ! Publish(navGoalTopic, SString(next))
                  goto(Active) using SearchingData(act, asker, newVisited, item)
                case None =>
                  log.error("No places left to visit.")
                  asker ! Failed(act)
                  goto(Idle) using IdleData
              }

          }

        case x =>
          log.error("Unhandled: "+x)
          stay()
      }
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

  def getNextToVisit(visited: Set[String]) = {
    Database.roomInstances.filterNot(visited.contains(_)).headOption
  }
}

