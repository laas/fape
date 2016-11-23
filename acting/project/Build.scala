
import sbt._
import Keys._

object FapeActingBuild extends Build {

  lazy val fapeActing = Project("fape-acting", file(".")) dependsOn(rosScala, fapePlanning)

  lazy val rosScala = RootProject(file("../ros-scala"))

  lazy val fapePlanning = ProjectRef(file("../fape-planning"), "fape-planning")

}