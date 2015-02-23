name := "constraints"

organization := "fr.laas.fape"

version := "0.8-SNAPSHOT"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

libraryDependencies += "fr.laas.fape" % "graphs" % "0.8-SNAPSHOT"

crossPaths := false

