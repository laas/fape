name := "constraints"

organization := "fr.laas.fape"

version := "0.8-SNAPSHOT"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "test"

libraryDependencies ++= Seq(
  "fr.laas.fape" % "graphs" % "0.8-SNAPSHOT",
  "net.openhft" % "koloboke-api-jdk6-7" % "0.6.7")



crossPaths := false

