name := "planstack-anml"

organization := "planstack"

version := "0.3.7"

scalaVersion := "2.10.3"

resolvers += "planstack-maven" at "http://planstack.github.io/repository/maven"

libraryDependencies += "planstack" % "planstack-graph" % "0.3.2"

crossPaths := false


// setup plugin for javadoc. Javadoc can be generated with task genjavadoc:doc
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


// Publish to local repository which should map to the https://github.com/planstack/repository
publishTo := Some(Resolver.file("file", new File("../repository/maven")))

publishMavenStyle := true
