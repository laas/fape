package fape.scenarios.morse

import adream_actions.Surface
import fape.acting.Time

import scala.collection.immutable

case class TimeStampedLocation(time: Int, loc: String) {
  override def toString = s"($time, $loc)"
}

object Database {

  @volatile private var surfaceLocations: Option[immutable.Map[String, (Double, Double, Double)]] = None

  @volatile private var locations = Map[String, List[TimeStampedLocation]]()

  @volatile var robotInstances = Set[String]()
  @volatile var itemInstances = Set[String]()
  @volatile var roomInstances = Set[String]()

  def updateLocations(locs: Iterable[(String, String)]): Unit = {
    var tmp = locations
    for((id, loc) <- locs) {
      tmp =
        if(!tmp.contains(id) || tmp(id).isEmpty || tmp(id).head.loc != loc)
          tmp.updated(id, TimeStampedLocation(Time.now, loc) :: tmp.getOrElse(id, Nil))
        else
          tmp
    }
    locations = tmp
  }

  def updateSurfaceLocations(tables: Iterable[Surface]): Unit = {
    var tmp = immutable.Map[String, (Double, Double, Double)]()
    for(surface <- tables) {
      val coords = surface.getPosition
      tmp = tmp.updated(surface.getLabel, (coords.getX, coords.getY, 2.0))
    }
    surfaceLocations = Some(tmp)
  }

  def lastLocation(id: String) : Option[TimeStampedLocation] = locations.get(id).flatMap(_.headOption)

  def locationsAsAnml : String = {
    var sb = StringBuilder.newBuilder

    for (r <- robotInstances) {
      val carriedObject =
        itemInstances
          .map(i => lastLocation(i))
          .filter(_.nonEmpty)
          .map(_.get)
          .find(_.loc == r)

      lastLocation(r) match {
        case Some(TimeStampedLocation(i, loc)) => sb ++= s"[$i] $r.location := $loc;\n"
        case None => sb ++= s"[start] $r.location := unknown;\n"
      }
      carriedObject match {
        case Some(TimeStampedLocation(time, _)) => sb ++= s"[$time] $r.empty := false;\n"
        case None => sb ++= s"[${Time.now}] $r.empty := true;\n" //TODO: was before "now"
      }

      for(i <- itemInstances) {
        lastLocation(i) match {
          case Some(TimeStampedLocation(time, loc)) => sb ++= s"[$time] $i.location := $loc;\n"
          case None => sb ++= s"[start] $i.location := unknown;\n"
        }
      }

    }
    sb.toString()
  }

  def getSurfaceLocations = surfaceLocations

}
