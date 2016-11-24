name := "fape"

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

lazy val commonSettings = Seq(
  organization := "fr.laas.fape",
  version := "12",
  crossPaths := true,
  exportJars := true, // insert other project dependencies in oneJar
  scalaVersion := "2.11.6",
  javaOptions in run ++= Seq("-Xmx3000m", "-ea"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  resolvers += "FAPE Nightly Maven Repo" at "http://www.laas.fr/~abitmonn/maven/",
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("org", "w3c", xs @ _*)         => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val fapeActing = Project("acting", file("acting"))
     .aggregate(fapePlanning, graphs, constraints, anml, svgPlot, structures)
     .dependsOn(fapePlanning, graphs, constraints, anml, svgPlot, structures)
     .settings(commonSettings: _*)

lazy val fapePlanning = Project("planning", file("planning"))
     .aggregate(graphs, constraints, anml, svgPlot, structures)
     .dependsOn(graphs, constraints, anml, svgPlot, structures)
     .settings(commonSettings: _*)

lazy val graphs = Project("graphs", file("graphs"))
     .settings(commonSettings: _*)

lazy val constraints = Project("constraints", file("constraints"))
     .aggregate(graphs, anml, structures)
     .dependsOn(graphs, anml, structures)
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

packSettings

packMain := Map(
  "fape" -> "fr.laas.fape.planning.Planning",
  "fape-server" -> "fr.laas.fape.planning.Server"
)

packJvmOpts := Map(
  "fape" -> Seq("-ea"),
  "fape-server" -> Seq()
)


