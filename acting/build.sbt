name := "acting"

organization := "fr.laas.fape"

resolvers += "ROS Java" at "https://github.com/rosjava/rosjava_mvn_repo/raw/master"

resolvers += "Local ROS Messages" at "file:///home/abitmonn/catkin_ws/devel/share/maven"

// libraryDependencies ++= Seq(
//   "org.ros.rosjava_core" % "rosjava" % "latest.integration",
//   "org.ros.rosjava_messages" % "move_base_msgs" % "latest.integration" //"1.11.14"
// )

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "latest.integration",
  // akka to use slf4j, config in resources/application.conf
  "com.typesafe.akka" % "akka-slf4j_2.11" % "2.3.9",
  // Logger implementation for slf4j. Configuration in resources/logback.xml
  "ch.qos.logback" % "logback-classic" % "1.1.2"
  // to make common logging (used by rosjava) use slf4j. Sees resources/common-properties
//  "org.slf4j" % "jcl-over-slf4j" % "1.7.10"
)



libraryDependencies += "com.typesafe.play" %% "play-json" % "2.3.4"

libraryDependencies += "org.ros.rosjava_core" % "rosjava" % "0.2.0"

libraryDependencies += "org.ros.rosjava_messages" % "actionlib_tutorials" % "0.1.8"

libraryDependencies += "org.ros.rosjava_messages" % "gtp_ros_msg" % "0.0.0"

libraryDependencies += "org.ros.rosjava_messages" % "pr2motion" % "0.0.0"

libraryDependencies += "org.ros.rosjava_messages" % "toaster_msgs" % "0.0.0"

libraryDependencies += "org.ros.rosjava_messages" % "move_base_msgs" % "1.12.7"

libraryDependencies += "org.ros.rosjava_messages" % "gazebo_msgs" % "2.4.10"
