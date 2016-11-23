name := "fape-acting"

organization := "fr.laas.fape"

resolvers += "ROS Java" at "https://github.com/rosjava/rosjava_mvn_repo/raw/master"

resolvers += "ROS WS" at "file://" + sys.env("ROS_MAVEN_DEPLOYMENT_REPOSITORY")


libraryDependencies ++= Seq(
  "org.ros.rosjava_core" % "rosjava" % "latest.integration",
  "com.github.rosjava.adream_java_messages" % "adream_actions" % "0.0.1",
  "org.ros.rosjava_messages" % "move_base_msgs" % "latest.integration" //"1.11.14"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.10" % "latest.integration",
  // akka to use slf4j, config in resources/application.conf
  "com.typesafe.akka" % "akka-slf4j_2.10" % "2.3.9",
  // Logger implementation for slf4j. Configuration in resources/logback.xml
  "ch.qos.logback" % "logback-classic" % "1.1.2"
  // to make common logging (used by rosjava) use slf4j. Sees resources/common-properties
//  "org.slf4j" % "jcl-over-slf4j" % "1.7.10"
)
