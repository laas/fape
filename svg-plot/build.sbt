name := "svg-plot"

organization := "fr.laas.fape"

version := "0.1-SNAPSHOT"

scalaVersion := "2.10.5"

libraryDependencies ++= Seq(
  "batik" % "batik-swing" % "1.6-1"
)

crossPaths := true