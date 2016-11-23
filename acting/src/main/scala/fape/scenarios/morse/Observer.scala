package fape.scenarios.morse

import akka.actor.{FSM, Actor}
import akka.event.Logging
import fape.acting.ActivityManager.{SetGoal, Tick}
import fape.acting.messages.{ErrorOnProblemGeneration, ProblemFromScene, GetProblemFromScene}
import fape.actors.patterns.{MessageLogger, DelayedForwarder}
import org.ros.scala.message.generated.adream_actions._
import org.ros.scala.node.{ServiceRequest, ServiceResponse}

import scala.collection.JavaConverters._
import scala.concurrent._
import scala.concurrent.duration._

import Observer._

object Observer {
  trait State
  case object Uninitalized extends State
  case object Initialized extends State

  case object InitDone

  case object GetAllValues
}

class Observer extends FSM[State, Any] with DelayedForwarder {
  import context.dispatcher

  val passer = context.actorSelection("../passer")
  val manager = context.actorSelection("../manager")

  @volatile private var currentRequest = 0
  private var robotsPromise = promise[SRobotsResponse]()
  private var itemsPromise = promise[SItemsResponse]()
  private var roomsPromise = promise[SRoomsResponse]()
  private var surfacesPromise = promise[SSurfacesResponse]()



  private var count = 0

  startWith(Uninitalized, 0)

  when(Uninitalized) {
    case Event(GetProblemFromScene, _) =>
      log.info("Requesting scene information")

      passer ! ServiceRequest(Topics.robotsService, SRobotsRequest(""), currentRequest)
      passer ! ServiceRequest(Topics.itemsService, SItemsRequest(""), currentRequest)
      passer ! ServiceRequest(Topics.roomsService, SRoomsRequest(""), currentRequest)
      passer ! ServiceRequest(Topics.surfacesService, SSurfacesRequest(""), currentRequest)

      future {
        val anmlFuture = for (robots <- robotsPromise.future;
                              items <- itemsPromise.future;
                              rooms <- roomsPromise.future;
                              surfaces <- surfacesPromise.future) yield {
          Database.updateSurfaceLocations(surfaces.surfaces.asScala)
          var anml = ""
          for (SRobot(name, room, _, _) <- robots.getRobots.asScala) {
            anml += s"instance Robot $name;\n"
          }
          for (SRoom(name) <- rooms.rooms.asScala) {
            anml += s"instance Room $name;\n"
            for (SRoom(name2) <- rooms.rooms.asScala) {
              anml += s"navigable($name, $name2) := true;\n"
              anml += s"navigable($name2, $name) := true;\n"
            }
          }
          for (SSurface(name, room, typ, pos) <- surfaces.surfaces.asScala) {
            anml += s"instance Table $name;\n"
            anml += s"$name.room := $room;\n"
          }
          for (SItem(name, room, location, pos) <- items.locations.asScala) {
            anml += s"instance Item $name;\n"
          }
          anml
        }

        // to be executed on future's completion
        val nominal = anmlFuture.map(anml => () => {
          log.info("Loading ANML domain")
          currentRequest += 1 // TODO potential race condition, create more futures
          self ! InitDone
          manager ! ProblemFromScene(anml)
        })
        // to be executed if future takes too long
        val fallback = () => {
          log.error(s"Some sensor for building the anml domain did not answer in time.")
          currentRequest += 1
          manager ! ErrorOnProblemGeneration
        }
        doFirstOf(nominal, 15.seconds, fallback)

      }
      stay()

    case Event(ServiceResponse(_, SRobotsResponse(robots), reqID), _) if reqID == currentRequest =>
      Database.robotInstances = robots.asScala.map(r => r.getLabel).toSet
      robotsPromise.success(SRobotsResponse(robots))
      stay()

    case Event(ServiceResponse(_, SRoomsResponse(rooms), reqID), _) if reqID == currentRequest =>
      Database.roomInstances = rooms.asScala.map(r => r.getLabel).toSet
      roomsPromise.success(SRoomsResponse(rooms))
      stay()

    case Event(ServiceResponse(_, SSurfacesResponse(surfaces), reqID), _) if reqID == currentRequest =>
      surfacesPromise.success(SSurfacesResponse(surfaces))
      stay()

    case Event(ServiceResponse(Topics.itemsService, SItemsResponse(items), reqID), _) if reqID == currentRequest =>
      Database.itemInstances = items.asScala.map(i => i.getItem).toSet
      itemsPromise.success(SItemsResponse(items))
      stay()

    case Event(InitDone, _) =>
      context.system.scheduler.schedule(0.seconds, 1.seconds, self, Tick)
      goto(Initialized)

      // we are not ready yet, save this request for later
    case Event(GetAllValues, _) =>
      forwardLater(500.milliseconds, GetAllValues, self)(sender())
      stay()
  }

  when(Initialized) {

    case Event(Tick, _) =>
      passer ! ServiceRequest(Topics.localItemsService, SItemsRequest(""), -1)
      passer ! ServiceRequest(Topics.robotsService, SRobotsRequest(""), -1)
      stay()

    case Event(ServiceResponse(Topics.localItemsService, SItemsResponse(items), -1), _) =>
      val itemLocations = items.asScala.map(i => (i.getItem, i.getLocation))
      Database.updateLocations(itemLocations)
      stay()

    case Event(ServiceResponse(Topics.robotsService, SRobotsResponse(robots), -1), _) =>
      val robotsLocations = robots.asScala.map(r => (r.getLabel, r.getRoom))
      Database.updateLocations(robotsLocations)
      stay()

    case Event(GetAllValues, _) =>
      sender() ! SetGoal(Database.locationsAsAnml)
      sender() ! SetGoal("[60] MyCornflakes.location == desk2; [120] MyMuesli.location == chest1;")
      stay()

  }


  whenUnhandled {
    case x =>
      log.error(s"Unhandled: $x")
      stay()
  }
}
