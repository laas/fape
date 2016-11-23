package fape.acting

import akka.actor.{Props, ActorSystem}
import geometry_msgs.{Quaternion, Point}
import org.jboss.netty.buffer.{ChannelBufferOutputStream, ChannelBuffers, ChannelBuffer}
import org.ros.internal.loader.CommandLineLoader
import org.ros.internal.message.RawMessage
import org.ros.message.Time
import org.ros.node.{DefaultNodeMainExecutor, NodeConfiguration}
import org.ros.scala.message.AbsMsg
import org.ros.scala.message.generated.geometry_msgs._
import org.ros.scala.message.generated.geometry_msgs.SPoint
import org.ros.scala.message.generated.geometry_msgs.SPose
import org.ros.scala.message.generated.geometry_msgs.SVector3
import org.ros.scala.message.generated.geometry_msgs.SWrench
import org.ros.scala.message.generated.nav_msgs.SMapMetaData
import org.ros.scala.message.generated.nav_msgs.SMapMetaData
import org.ros.scala.node._
import org.ros.scala.node.Publish
import scala.beans.BeanProperty
import scala.collection.{JavaConverters, JavaConversions}
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import org.ros.scala.message.generated.std_msgs.SString
import org.ros.scala.message.generated.adream_actions.SPlaceRequest

object FAPE extends App {


//  case class SPoint(@BeanProperty var x: Double, @BeanProperty var y: Double, @BeanProperty var z: Double)
//    extends AbsMsg with geometry_msgs.Point
//
//  case class SQuaternion(@BeanProperty var x: Double, @BeanProperty var y: Double, @BeanProperty var z: Double, @BeanProperty var w: Double)
//    extends AbsMsg with geometry_msgs.Quaternion
//
//  case class SPose(@BeanProperty var position: Point, @BeanProperty var orientation: Quaternion)
//    extends AbsMsg with geometry_msgs.Pose

  val system = ActorSystem("FAPESystem")

  val passer = system.actorOf(Props[ROSMessagePasser], name = "passer")

//  val manager = system.actorOf(Props[fape.acting.PlanManager], name = "fape.acting.PlanManager")
//  val actor = system.actorOf(Props[fape.scenarios.morse.MainActor], name = "actor")

  val placer = system.actorOf(Props(classOf[fape.acting.PickPlaceFSM], "pr2"), name = "placer")

  import system.dispatcher
  system.scheduler.scheduleOnce(2 seconds, placer, DoPick("pr2", "MyCornflakes"))
//  system.scheduler.scheduleOnce(5000 milliseconds, manager, "start")
//  system.scheduler.scheduleOnce(5000 milliseconds, placer, "start")
  //manager ! "start"

//  system.scheduler.scheduleOnce(2 seconds, passer, Publish("/pr2/nav_goal", SString("coucou")))
//  system.scheduler.scheduleOnce(2 seconds, passer, Publish("/test3", SPose(SPoint(1,2,3), SQuaternion(1,2,3,4))))
//  system.scheduler.scheduleOnce(2 seconds, passer, Publish("/test4", SWrench(SVector3(14.2, 5, 3), SVector3(4, 5, 9))))
//  system.scheduler.scheduleOnce(4 seconds, passer, Publish("/test4", SWrench(SVector3(14.2, 5, 3), SVector3(4, 5, 9))))
//  val meta = SMapMetaData(new Time(3), 4, 4, 4, SPose(SPoint(1,2,3), SQuaternion(4, 5, 9, 10)))
//  system.scheduler.scheduleOnce(2 seconds, passer, Publish("/test5", meta))
//  system.scheduler.scheduleOnce(4 seconds, passer, Publish("/test5", meta))
//
//
//  val buf = ChannelBuffers.buffer(1024)
//  val out = new ChannelBufferOutputStream(buf)
//  out.writeChars("coucou")

//  val msg = SPlaceRequest(1, 2, 3)
//  for(f <- msg.toRawMessage.getFields)
//    f.serialize(buf)
//
//  println(buf.toString(0, 12, "UTF-8"))
}
