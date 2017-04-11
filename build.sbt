name := "fape"

// global settings 
val _organization = "fr.laas.fape"
val _version = "1.1-SNAPSHOT"
val _scalaVersion = "2.12.1"


version := _version
scalaVersion := _scalaVersion
organization := _organization
crossPaths := false

initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

lazy val commonSettings = Seq(
  organization := _organization,
  version := _version,
  crossPaths := false,
  exportJars := true, // insert other project dependencies in oneJar
  scalaVersion := _scalaVersion,
  javaOptions in run ++= Seq("-Xmx3000m", "-ea"),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("org", "w3c", xs @ _*)         => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"	
)

lazy val fapeActing = Project("acting", file("acting"))
     .aggregate(fapePlanning, constraints, anml, svgPlot, structures)
     .dependsOn(fapePlanning, constraints, anml, svgPlot, structures)
     .settings(commonSettings: _*)

lazy val fapePlanning = Project("planning", file("planning"))
     .aggregate(constraints, anml, svgPlot, structures)
     .dependsOn(constraints, anml, svgPlot, structures)
     .settings(commonSettings: _*)

lazy val constraints = Project("constraints", file("constraints"))
     .aggregate(anml, structures)
     .dependsOn(anml, structures)
     .settings(commonSettings: _*)

lazy val anml = Project("anml-parser", file("anml-parser"))
     .aggregate(structures)
     .dependsOn(structures)
     .settings(commonSettings: _*)
     .settings(libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4")

lazy val svgPlot = Project("svg-plot", file("svg-plot"))
     .settings(commonSettings: _*)
     .settings(libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6")

lazy val structures = Project("structures", file("structures"))
     .settings(commonSettings: _*)

lazy val lsp = Project("local-search-planner", file("local-search-planner"))
     .aggregate(fapePlanning, constraints, anml, svgPlot, structures)
     .dependsOn(fapePlanning, constraints, anml, svgPlot, structures)
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


