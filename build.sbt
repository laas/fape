name := "fape"

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

lazy val commonSettings = Seq(
  organization := "fr.laas.fape",
  version := "12-SNAPSHOT",
  crossPaths := true,
  exportJars := true, // insert other project dependencies in oneJar
  scalaVersion := "2.11.6",
  javaOptions in run ++= Seq("-Xmx3000m", "-ea"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  resolvers += "FAPE Nightly Maven Repo" at "http://www.laas.fr/~abitmonn/maven/"
)

lazy val fapePlanning = Project("planning", file("planning"))
     .aggregate(graphs, constraints, anml, svgPlot, structures)
     .dependsOn(graphs, constraints, anml, svgPlot, structures)
     .settings(commonSettings: _*)

lazy val graphs = Project("graphs", file("graphs"))
     .settings(commonSettings: _*)

lazy val constraints = Project("constraints", file("constraints"))
     .aggregate(graphs, anml)
     .dependsOn(graphs, anml)
     .settings(commonSettings: _*)

lazy val anml = Project("anml-parser", file("anml-parser"))
     .aggregate(graphs)
     .dependsOn(graphs)
     .settings(commonSettings: _*)
     .settings(libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4")

lazy val svgPlot = Project("svg-plot", file("svg-plot"))
     .settings(commonSettings: _*)
     .settings(libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.3")

lazy val structures = Project("structures", file("structures"))
     .settings(commonSettings: _*)


libraryDependencies ++= Seq(
  "net.openhft" % "koloboke-api-jdk6-7" % "0.6.7" % "runtime")

packSettings

packMain := Map(
  "fape" -> "fape.Planning",
  "fape-server" -> "fape.Server"
)

packJvmOpts := Map(
  "fape" -> Seq("-ea"),
  "fape-server" -> Seq()
)


