name := "anml-parser"

organization := "fr.laas.fape"

version := "0.7.0"

resolvers += "planstack-maven" at "http://planstack.github.io/repository/maven"

libraryDependencies += "planstack" % "planstack-graph" % "[0.4.0,0.5)"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

crossPaths := false

