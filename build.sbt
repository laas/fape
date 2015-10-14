name := "constraints"

organization := "fr.laas.fape"

version := "0.8-SNAPSHOT"

resolvers += "FAPE Nightly Maven Repo" at "http://www.laas.fr/~abitmonn/maven/"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

libraryDependencies ++= Seq(
  "fr.laas.fape" %% "graphs" % "0.8-SNAPSHOT",
  "fr.laas.fape" %% "anml-parser" % "0.8-SNAPSHOT",
  "net.openhft" % "koloboke-api-jdk6-7" % "0.6.7",
  "net.openhft" % "koloboke-impl-jdk6-7" % "0.6.7" % "runtime"
//  "com.github.scala-blitz" %% "scala-blitz" % "1.1"
)

crossPaths := true