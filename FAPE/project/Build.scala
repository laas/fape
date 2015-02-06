import sbt._
import Keys._
import xerial.sbt.Pack._

object FapePlannerBuild extends Build {

    lazy val root = Project(
      id = "fape-planning",
      base = file("."),
      settings = Defaults.defaultSettings 
        ++ packSettings // This settings add pack and pack-archive commands to sbt
        ++ Seq(packMain := Map("fape-planner" -> "fape.Planning"))
      ) dependsOn(anml, constraints, graphs)

    lazy val graphs = RootProject(file("../planstack/graph"))

    lazy val constraints = RootProject(file("../planstack/constraints"))// dependsOn(graphs)

    lazy val anml = RootProject(file("../planstack/anml"))
}