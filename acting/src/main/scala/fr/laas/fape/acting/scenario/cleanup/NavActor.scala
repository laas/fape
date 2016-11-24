package fr.laas.fape.acting.scenario.cleanup

import akka.actor.{ActorRef, FSM}
import fr.laas.fape.acting.messages.{AAction, Execute, Failed, Success}
import fr.laas.fape.planning.core.execution.model.AtomicAction


object NavActor {
  // states
  sealed trait State
  case object Idle extends State
  case object Active extends State

  sealed trait Data
  case object NoGoal extends Data
  final case class CurrentGoal(act: AtomicAction, sender: ActorRef) extends Data
}
import NavActor._

class NavActor(robot: String) extends FSM[State,Data] {


  startWith(Idle, NoGoal)

  when(Idle) {
    case Event(Execute(aa), NoGoal) => aa match {
      case AAction(_, name, List(`robot`, from, to), start, minDur, maxDur) =>

        goto(Active) using CurrentGoal(aa, sender())
    }
  }

  when(Active) {
    case Event(x, CurrentGoal(act, sender)) => {
//      data.getData match {
//        case "done" =>
//          sender ! Success(act) //("Success", ("Move", robot, goal))
//        case "error" =>
//          log.warning(s"Error in nav_goal exec of $act")
//          sender ! Failed(act)
//        case "timeout" =>
//          log.warning(s"nav_goal $act timed out.")
//          sender ! Failed(act)
//        case x => log.info(x)
//      }
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