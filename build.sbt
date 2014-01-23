name := "planstack-constraints"

organization := "planstack"

version := "0.1"

scalaVersion := "2.10.3"

libraryDependencies += "choco" % "choco-solver" % "2.1.5"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

resolvers += "choco.repos" at "http://www.emn.fr/z-info/choco-repo/mvn/repository/"

lazy val graph = RootProject(file("../graph"))

lazy val root = Project(id = "constraints", base = file(".")) dependsOn (graph)

crossPaths := false
