
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "latest.integration",

  // akka to use slf4j, config in resources/application.conf
  "com.typesafe.akka" %% "akka-slf4j" % "latest.integration",

  // Logger implementation for slf4j. Configuration in resources/logback.xml
  "ch.qos.logback" % "logback-classic" % "1.1.2"

  // to make common logging (used by rosjava) use slf4j. Sees resources/common-properties
  //  "org.slf4j" % "jcl-over-slf4j" % "1.7.10"
)


