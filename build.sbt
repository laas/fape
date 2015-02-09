name := "planstack-anml"

organization := "planstack"

version := "0.6.8"

resolvers += "planstack-maven" at "http://planstack.github.io/repository/maven"

libraryDependencies += "planstack" % "planstack-graph" % "[0.4.0,0.5)"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

crossPaths := false

