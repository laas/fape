name := "graphs"

organization := "fr.laas.fape"

version := "0.8-SNAPSHOT"

resolvers += "FAPE Nightly Maven repo" at "http://www.laas.fr/~abitmonn/maven/"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"

crossPaths := true