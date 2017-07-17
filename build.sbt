name := "fape-build"

// global settings 
val _organization = "com.github.arthur-bit-monnot"
val _version = "1.0"
val _scalaVersion = "2.12.2"


initialize := {
  val _ = initialize.value
  if (sys.props("java.specification.version") != "1.8")
    sys.error("Java 8 is required for this project.")
}

lazy val commonSettings = Seq(
  organization := _organization,
  version := _version,
  crossPaths := true,
  exportJars := true, // insert other project dependencies in oneJar
  scalaVersion := _scalaVersion,
  javaOptions in run ++= Seq("-Xmx3000m", "-ea"),
  javacOptions in compile ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  javacOptions in doc ++= Seq("-source", "1.8", "-Xdoclint:none"),
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("org", "w3c", xs @ _*)         => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  // To sync with Maven central, you need to supply the following information:
  publishMavenStyle := true,

  // POM settings for Sonatype
  homepage := Some(url("https://github.com/arthur-bit-monnot/fape")),
  scmInfo := Some(ScmInfo(url("https://github.com/arthur-bit-monnot/fape"), "git@github.com:arthur-bit-monnot/fape.git")),
  developers += Developer("abitmonn", "Arthur Bit-Monnot", "arthur.bit-monnot@laas.fr", url("https://github.com/arthur-bit-monnot")),
  licenses += ("BSD-2-Clause", url("https://opensource.org/licenses/BSD-2-Clause")),
  pomIncludeRepository := (_ => false)
)

lazy val root = project.in(file(".")).
  aggregate(fapePlanning).

  settings(
    publish := {},

    publishLocal := {}
  )

lazy val fapeActing = Project("fape-acting", file("acting"))
     .aggregate(fapePlanning, constraints, anml, svgPlot, structures)
     .dependsOn(fapePlanning, constraints, anml, svgPlot, structures)
     .settings(commonSettings: _*)

lazy val fapePlanning = Project("fape-planning", file("planning"))
     .aggregate(constraints, anml, svgPlot, structures)
     .dependsOn(constraints, anml, svgPlot, structures)
     .settings(commonSettings: _*)
     .settings(crossPaths := false)  // disable cross path as this is a pure java project

lazy val constraints = Project("fape-constraints", file("constraints"))
     .aggregate(anml, structures)
     .dependsOn(anml, structures)
     .settings(commonSettings: _*)

lazy val anml = Project("fape-anml-parser", file("anml-parser"))
     .aggregate(structures)
     .dependsOn(structures)
     .settings(commonSettings: _*)
     .settings(libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4")

lazy val svgPlot = Project("fape-svg-plot", file("svg-plot"))
     .settings(commonSettings: _*)
     .settings(libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6")

lazy val structures = Project("fape-structures", file("structures"))
     .settings(commonSettings: _*)

packSettings

packMain := Map(
  "fape" -> "fr.laas.fape.planning.Planning"
)

packJvmOpts := Map(
  "fape" -> Seq("-ea")
)

// Add sonatype repository settings
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)