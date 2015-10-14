name := "anml-parser"

organization := "fr.laas.fape"

version := "0.8-SNAPSHOT"

resolvers += "FAPE Nightly Maven Repo" at "http://www.laas.fr/~abitmonn/maven/"

libraryDependencies ++= Seq(
  "fr.laas.fape" %% "graphs" % "0.8-SNAPSHOT",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)

crossPaths := true