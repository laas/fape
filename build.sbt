name := "fape-build"

// global settings 

inThisBuild(List(
    scalaVersion := "2.12.6",
    // These are normal sbt settings to configure for release, skip if already defined
    organization := "com.github.arthur-bit-monnot",
    licenses := Seq("BSD-2-Clause" -> url("https://opensource.org/licenses/BSD-2-Clause")),
    homepage := Some(url("https://github.com/laas/fape")),
    developers := List(Developer("arthur-bit-monnot", "Arthur Bit-Monnot", "arthur.bitmonnot@gmail.com", url("https://arthur-bit-monnot.github.io"))),
    scmInfo := Some(ScmInfo(url("https://github.com/laas/fape"), "scm:git:git@github.com:laas/fape.git")),

    // These are the sbt-release-early settings to configure
    pgpPublicRing := file("./travis/local.pubring.asc"),
    pgpSecretRing := file("./travis/local.secring.asc"),
    releaseEarlyEnableLocalReleases := true,
    releaseEarlyWith := SonatypePublisher
))

lazy val commonSettings = Seq(
  crossPaths := true,
  exportJars := true, // insert other project dependencies in oneJar
  javaOptions in run ++= Seq("-Xmx3000m", "-ea"),
  javacOptions in compile ++= Seq("-Xlint"),
  javacOptions in doc ++= Seq("-Xdoclint:none"),
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


