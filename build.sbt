name := "planstack-constraints"

organization := "planstack"

version := "0.4.4"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-deprecation", "-feature")

javacOptions += "-Xlint:unchecked"

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "2.0" % "test"

resolvers += "planstack-maven" at "http://planstack.github.io/repository/maven"

libraryDependencies += "planstack" % "planstack-graph" % "0.3.9"


crossPaths := false

val JavaDoc = config("genjavadoc") extend Compile

val javadocSettings = inConfig(JavaDoc)(Defaults.configSettings) ++ Seq(
  libraryDependencies += compilerPlugin("com.typesafe.genjavadoc" %%
    "genjavadoc-plugin" % "0.5" cross CrossVersion.full),
  scalacOptions <+= target map (t => "-P:genjavadoc:out=" + (t / "java")),
  packageDoc in Compile <<= packageDoc in JavaDoc,
  sources in JavaDoc <<=
    (target, compile in Compile, sources in Compile) map ((t, c, s) =>
      (t / "java" ** "*.java").get ++ s.filter(_.getName.endsWith(".java"))),
  javacOptions in JavaDoc := Seq(),
  artifactName in packageDoc in JavaDoc :=
    ((sv, mod, art) =>
      "" + mod.name + "_" + sv.binary + "-" + mod.revision + "-javadoc.jar")
)

seq(javadocSettings: _*)

publishTo := Some(Resolver.file("file", new File("../repository/maven")))
