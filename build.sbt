name := "planstack-constraints"

organization := "planstack"

version := "0.5.5"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

resolvers += "planstack-maven" at "http://planstack.github.io/repository/maven"

libraryDependencies += "planstack" % "planstack-graph" % "[0.4.0,0.5)"

crossPaths := false

