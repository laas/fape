package org.ros.scala.message.generated

import org.ros.scala.message.AbsMsg

import scala.beans.BeanProperty

package std_msgs {
  case class SInt32(
      @BeanProperty var data: Int)
    extends AbsMsg with _root_.std_msgs.Int32

  case class SUInt32(
      @BeanProperty var data: Int)
    extends AbsMsg with _root_.std_msgs.UInt32

  case class SBool(
      @BeanProperty var data: Boolean)
    extends AbsMsg with _root_.std_msgs.Bool

  case class SString(
      @BeanProperty var data: String)
    extends AbsMsg with _root_.std_msgs.String

  case class SDuration(
      @BeanProperty var data: org.ros.message.Duration)
    extends AbsMsg with _root_.std_msgs.Duration

  case class SUInt16MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Short])
    extends AbsMsg with _root_.std_msgs.UInt16MultiArray

  case class STime(
      @BeanProperty var data: org.ros.message.Time)
    extends AbsMsg with _root_.std_msgs.Time

  case class SByte(
      @BeanProperty var data: Byte)
    extends AbsMsg with _root_.std_msgs.Byte

  case class SMultiArrayDimension(
      @BeanProperty var label: String,
      @BeanProperty var size: Int,
      @BeanProperty var stride: Int)
    extends AbsMsg with _root_.std_msgs.MultiArrayDimension

  case class SFloat32(
      @BeanProperty var data: Float)
    extends AbsMsg with _root_.std_msgs.Float32

  case class SChar(
      @BeanProperty var data: Byte)
    extends AbsMsg with _root_.std_msgs.Char

  case class SUInt8(
      @BeanProperty var data: Byte)
    extends AbsMsg with _root_.std_msgs.UInt8

  case class SUInt32MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Int])
    extends AbsMsg with _root_.std_msgs.UInt32MultiArray

  case class SInt8(
      @BeanProperty var data: Byte)
    extends AbsMsg with _root_.std_msgs.Int8

  case class SHeader(
      @BeanProperty var seq: Int,
      @BeanProperty var stamp: org.ros.message.Time,
      @BeanProperty var frameId: String)
    extends AbsMsg with _root_.std_msgs.Header

  case class SUInt64MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Long])
    extends AbsMsg with _root_.std_msgs.UInt64MultiArray

  case class SMultiArrayLayout(
      @BeanProperty var dim: java.util.List[_root_.std_msgs.MultiArrayDimension],
      @BeanProperty var dataOffset: Int)
    extends AbsMsg with _root_.std_msgs.MultiArrayLayout

  case class SUInt8MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: org.jboss.netty.buffer.ChannelBuffer)
    extends AbsMsg with _root_.std_msgs.UInt8MultiArray

  case class SUInt64(
      @BeanProperty var data: Long)
    extends AbsMsg with _root_.std_msgs.UInt64

  case class SInt16MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Short])
    extends AbsMsg with _root_.std_msgs.Int16MultiArray

  case class SFloat64(
      @BeanProperty var data: Double)
    extends AbsMsg with _root_.std_msgs.Float64

  case class SInt16(
      @BeanProperty var data: Short)
    extends AbsMsg with _root_.std_msgs.Int16

  case class SInt32MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Int])
    extends AbsMsg with _root_.std_msgs.Int32MultiArray

  case class SInt64MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Long])
    extends AbsMsg with _root_.std_msgs.Int64MultiArray

  case class SInt8MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: org.jboss.netty.buffer.ChannelBuffer)
    extends AbsMsg with _root_.std_msgs.Int8MultiArray

  case class SColorRGBA(
      @BeanProperty var r: Float,
      @BeanProperty var g: Float,
      @BeanProperty var b: Float,
      @BeanProperty var a: Float)
    extends AbsMsg with _root_.std_msgs.ColorRGBA

  case class SUInt16(
      @BeanProperty var data: Short)
    extends AbsMsg with _root_.std_msgs.UInt16

  case class SInt64(
      @BeanProperty var data: Long)
    extends AbsMsg with _root_.std_msgs.Int64

  case class SFloat32MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Float])
    extends AbsMsg with _root_.std_msgs.Float32MultiArray

  case class SFloat64MultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: Array[Double])
    extends AbsMsg with _root_.std_msgs.Float64MultiArray

  case class SEmpty(
      )
    extends AbsMsg with _root_.std_msgs.Empty

  case class SByteMultiArray(
      @BeanProperty var layout: _root_.std_msgs.MultiArrayLayout,
      @BeanProperty var data: org.jboss.netty.buffer.ChannelBuffer)
    extends AbsMsg with _root_.std_msgs.ByteMultiArray}

package nav_msgs {
  case class SGetMapRequest(
      )
    extends AbsMsg with _root_.nav_msgs.GetMapRequest

  case class SGetMapFeedback(
      )
    extends AbsMsg with _root_.nav_msgs.GetMapFeedback

  case class SGetPlanResponse(
      @BeanProperty var plan: _root_.nav_msgs.Path)
    extends AbsMsg with _root_.nav_msgs.GetPlanResponse

  case class SGetMapResult(
      @BeanProperty var map: _root_.nav_msgs.OccupancyGrid)
    extends AbsMsg with _root_.nav_msgs.GetMapResult

  case class SOccupancyGrid(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var info: _root_.nav_msgs.MapMetaData,
      @BeanProperty var data: org.jboss.netty.buffer.ChannelBuffer)
    extends AbsMsg with _root_.nav_msgs.OccupancyGrid

  case class SOdometry(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var childFrameId: String,
      @BeanProperty var pose: _root_.geometry_msgs.PoseWithCovariance,
      @BeanProperty var twist: _root_.geometry_msgs.TwistWithCovariance)
    extends AbsMsg with _root_.nav_msgs.Odometry

  case class SGetMapActionResult(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var status: _root_.actionlib_msgs.GoalStatus,
      @BeanProperty var result: _root_.nav_msgs.GetMapResult)
    extends AbsMsg with _root_.nav_msgs.GetMapActionResult

  case class SGetMapGoal(
      )
    extends AbsMsg with _root_.nav_msgs.GetMapGoal

  case class SGetPlanRequest(
      @BeanProperty var start: _root_.geometry_msgs.PoseStamped,
      @BeanProperty var goal: _root_.geometry_msgs.PoseStamped,
      @BeanProperty var tolerance: Float)
    extends AbsMsg with _root_.nav_msgs.GetPlanRequest

  case class SGetMapResponse(
      @BeanProperty var map: _root_.nav_msgs.OccupancyGrid)
    extends AbsMsg with _root_.nav_msgs.GetMapResponse

  case class SGridCells(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var cellWidth: Float,
      @BeanProperty var cellHeight: Float,
      @BeanProperty var cells: java.util.List[_root_.geometry_msgs.Point])
    extends AbsMsg with _root_.nav_msgs.GridCells

  case class SPath(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var poses: java.util.List[_root_.geometry_msgs.PoseStamped])
    extends AbsMsg with _root_.nav_msgs.Path

  case class SGetMapActionGoal(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var goalId: _root_.actionlib_msgs.GoalID,
      @BeanProperty var goal: _root_.nav_msgs.GetMapGoal)
    extends AbsMsg with _root_.nav_msgs.GetMapActionGoal

  case class SGetMapAction(
      @BeanProperty var actionGoal: _root_.nav_msgs.GetMapActionGoal,
      @BeanProperty var actionResult: _root_.nav_msgs.GetMapActionResult,
      @BeanProperty var actionFeedback: _root_.nav_msgs.GetMapActionFeedback)
    extends AbsMsg with _root_.nav_msgs.GetMapAction

  case class SMapMetaData(
      @BeanProperty var mapLoadTime: org.ros.message.Time,
      @BeanProperty var resolution: Float,
      @BeanProperty var width: Int,
      @BeanProperty var height: Int,
      @BeanProperty var origin: _root_.geometry_msgs.Pose)
    extends AbsMsg with _root_.nav_msgs.MapMetaData

  case class SGetMapActionFeedback(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var status: _root_.actionlib_msgs.GoalStatus,
      @BeanProperty var feedback: _root_.nav_msgs.GetMapFeedback)
    extends AbsMsg with _root_.nav_msgs.GetMapActionFeedback}

package geometry_msgs {
  case class SQuaternion(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var z: Double,
      @BeanProperty var w: Double)
    extends AbsMsg with _root_.geometry_msgs.Quaternion

  case class STransform(
      @BeanProperty var translation: _root_.geometry_msgs.Vector3,
      @BeanProperty var rotation: _root_.geometry_msgs.Quaternion)
    extends AbsMsg with _root_.geometry_msgs.Transform

  case class SWrenchStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var wrench: _root_.geometry_msgs.Wrench)
    extends AbsMsg with _root_.geometry_msgs.WrenchStamped

  case class SPoint32(
      @BeanProperty var x: Float,
      @BeanProperty var y: Float,
      @BeanProperty var z: Float)
    extends AbsMsg with _root_.geometry_msgs.Point32

  case class SPoseStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var pose: _root_.geometry_msgs.Pose)
    extends AbsMsg with _root_.geometry_msgs.PoseStamped

  case class SVector3(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var z: Double)
    extends AbsMsg with _root_.geometry_msgs.Vector3

  case class SPose2D(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var theta: Double)
    extends AbsMsg with _root_.geometry_msgs.Pose2D

  case class SPoseWithCovariance(
      @BeanProperty var pose: _root_.geometry_msgs.Pose,
      @BeanProperty var covariance: Array[Double])
    extends AbsMsg with _root_.geometry_msgs.PoseWithCovariance

  case class SQuaternionStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var quaternion: _root_.geometry_msgs.Quaternion)
    extends AbsMsg with _root_.geometry_msgs.QuaternionStamped

  case class SWrench(
      @BeanProperty var force: _root_.geometry_msgs.Vector3,
      @BeanProperty var torque: _root_.geometry_msgs.Vector3)
    extends AbsMsg with _root_.geometry_msgs.Wrench

  case class SPose(
      @BeanProperty var position: _root_.geometry_msgs.Point,
      @BeanProperty var orientation: _root_.geometry_msgs.Quaternion)
    extends AbsMsg with _root_.geometry_msgs.Pose

  case class SPoint(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var z: Double)
    extends AbsMsg with _root_.geometry_msgs.Point

  case class SPolygon(
      @BeanProperty var points: java.util.List[_root_.geometry_msgs.Point32])
    extends AbsMsg with _root_.geometry_msgs.Polygon

  case class SPointStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var point: _root_.geometry_msgs.Point)
    extends AbsMsg with _root_.geometry_msgs.PointStamped

  case class STwistWithCovariance(
      @BeanProperty var twist: _root_.geometry_msgs.Twist,
      @BeanProperty var covariance: Array[Double])
    extends AbsMsg with _root_.geometry_msgs.TwistWithCovariance

  case class SVector3Stamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var vector: _root_.geometry_msgs.Vector3)
    extends AbsMsg with _root_.geometry_msgs.Vector3Stamped

  case class SPoseWithCovarianceStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var pose: _root_.geometry_msgs.PoseWithCovariance)
    extends AbsMsg with _root_.geometry_msgs.PoseWithCovarianceStamped

  case class STransformStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var childFrameId: String,
      @BeanProperty var transform: _root_.geometry_msgs.Transform)
    extends AbsMsg with _root_.geometry_msgs.TransformStamped

  case class STwist(
      @BeanProperty var linear: _root_.geometry_msgs.Vector3,
      @BeanProperty var angular: _root_.geometry_msgs.Vector3)
    extends AbsMsg with _root_.geometry_msgs.Twist

  case class SPoseArray(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var poses: java.util.List[_root_.geometry_msgs.Pose])
    extends AbsMsg with _root_.geometry_msgs.PoseArray

  case class STwistWithCovarianceStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var twist: _root_.geometry_msgs.TwistWithCovariance)
    extends AbsMsg with _root_.geometry_msgs.TwistWithCovarianceStamped

  case class STwistStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var twist: _root_.geometry_msgs.Twist)
    extends AbsMsg with _root_.geometry_msgs.TwistStamped

  case class SPolygonStamped(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var polygon: _root_.geometry_msgs.Polygon)
    extends AbsMsg with _root_.geometry_msgs.PolygonStamped}

package tf2_msgs {
  case class SFrameGraphResponse(
      @BeanProperty var frameYaml: String)
    extends AbsMsg with _root_.tf2_msgs.FrameGraphResponse

  case class STFMessage(
      @BeanProperty var transforms: java.util.List[_root_.geometry_msgs.TransformStamped])
    extends AbsMsg with _root_.tf2_msgs.TFMessage

  case class SFrameGraphRequest(
      )
    extends AbsMsg with _root_.tf2_msgs.FrameGraphRequest

  case class STF2Error(
      @BeanProperty var error: Byte,
      @BeanProperty var errorString: String)
    extends AbsMsg with _root_.tf2_msgs.TF2Error}

package rosgraph_msgs {
  case class SClock(
      @BeanProperty var clock: org.ros.message.Time)
    extends AbsMsg with _root_.rosgraph_msgs.Clock

  case class SLog(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var level: Byte,
      @BeanProperty var name: String,
      @BeanProperty var msg: String,
      @BeanProperty var file: String,
      @BeanProperty var function: String,
      @BeanProperty var line: Int,
      @BeanProperty var topics: java.util.List[String])
    extends AbsMsg with _root_.rosgraph_msgs.Log}

package rosjava_test_msgs {
  case class SAddTwoIntsResponse(
      @BeanProperty var sum: Long)
    extends AbsMsg with _root_.rosjava_test_msgs.AddTwoIntsResponse

  case class STestString(
      @BeanProperty var callerId: String,
      @BeanProperty var origCallerId: String,
      @BeanProperty var data: String)
    extends AbsMsg with _root_.rosjava_test_msgs.TestString

  case class SAddTwoIntsRequest(
      @BeanProperty var a: Long,
      @BeanProperty var b: Long)
    extends AbsMsg with _root_.rosjava_test_msgs.AddTwoIntsRequest

  case class STestPrimitives(
      @BeanProperty var callerId: String,
      @BeanProperty var origCallerId: String,
      @BeanProperty var str: String,
      @BeanProperty var b: Byte,
      @BeanProperty var int16: Short,
      @BeanProperty var int32: Int,
      @BeanProperty var int64: Long,
      @BeanProperty var c: Byte,
      @BeanProperty var uint16: Short,
      @BeanProperty var uint32: Int,
      @BeanProperty var uint64: Long,
      @BeanProperty var float32: Float,
      @BeanProperty var float64: Double,
      @BeanProperty var t: org.ros.message.Time,
      @BeanProperty var d: org.ros.message.Duration)
    extends AbsMsg with _root_.rosjava_test_msgs.TestPrimitives

  case class STestHeader(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var callerId: String,
      @BeanProperty var origCallerId: String,
      @BeanProperty var autoHeader: Byte)
    extends AbsMsg with _root_.rosjava_test_msgs.TestHeader

  case class SComposite(
      @BeanProperty var a: _root_.rosjava_test_msgs.CompositeA,
      @BeanProperty var b: _root_.rosjava_test_msgs.CompositeB)
    extends AbsMsg with _root_.rosjava_test_msgs.Composite

  case class SCompositeA(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var z: Double,
      @BeanProperty var w: Double)
    extends AbsMsg with _root_.rosjava_test_msgs.CompositeA

  case class SCompositeB(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var z: Double)
    extends AbsMsg with _root_.rosjava_test_msgs.CompositeB

  case class STestArrays(
      @BeanProperty var callerId: String,
      @BeanProperty var origCallerId: String,
      @BeanProperty var int32Array: Array[Int],
      @BeanProperty var float32Array: Array[Float],
      @BeanProperty var timeArray: java.util.List[org.ros.message.Time],
      @BeanProperty var testStringArray: java.util.List[_root_.rosjava_test_msgs.TestString])
    extends AbsMsg with _root_.rosjava_test_msgs.TestArrays}

package adream_actions {
  case class SPickResponse(
      @BeanProperty var status: Int)
    extends AbsMsg with _root_.adream_actions.PickResponse

  case class SItemsRequest(
      @BeanProperty var unused: String)
    extends AbsMsg with _root_.adream_actions.ItemsRequest

  case class SRobotsResponse(
      @BeanProperty var robots: java.util.List[_root_.adream_actions.Robot])
    extends AbsMsg with _root_.adream_actions.RobotsResponse

  case class SRoom(
      @BeanProperty var label: String)
    extends AbsMsg with _root_.adream_actions.Room

  case class SSurfacesRequest(
      @BeanProperty var unused: String)
    extends AbsMsg with _root_.adream_actions.SurfacesRequest

  case class SReleaseRequest(
      @BeanProperty var unused: String)
    extends AbsMsg with _root_.adream_actions.ReleaseRequest

  case class SItemsResponse(
      @BeanProperty var locations: java.util.List[_root_.adream_actions.Item])
    extends AbsMsg with _root_.adream_actions.ItemsResponse

  case class SGoToRequest(
      @BeanProperty var x: Float,
      @BeanProperty var y: Float,
      @BeanProperty var yaw: Float)
    extends AbsMsg with _root_.adream_actions.GoToRequest

  case class SGoToResponse(
      @BeanProperty var status: Int)
    extends AbsMsg with _root_.adream_actions.GoToResponse

  case class SReleaseResponse(
      @BeanProperty var status: Int)
    extends AbsMsg with _root_.adream_actions.ReleaseResponse

  case class SRoomsResponse(
      @BeanProperty var rooms: java.util.List[_root_.adream_actions.Room])
    extends AbsMsg with _root_.adream_actions.RoomsResponse

  case class SSurface(
      @BeanProperty var label: String,
      @BeanProperty var room: String,
      @BeanProperty var `type`: String,
      @BeanProperty var position: _root_.adream_actions.Point)
    extends AbsMsg with _root_.adream_actions.Surface

  case class SRoomsRequest(
      @BeanProperty var unused: String)
    extends AbsMsg with _root_.adream_actions.RoomsRequest

  case class SItem(
      @BeanProperty var item: String,
      @BeanProperty var room: String,
      @BeanProperty var location: String,
      @BeanProperty var position: _root_.adream_actions.Point)
    extends AbsMsg with _root_.adream_actions.Item

  case class SQuaternion(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var z: Double,
      @BeanProperty var w: Double)
    extends AbsMsg with _root_.adream_actions.Quaternion

  case class SPlaceRequest(
      @BeanProperty var x: Float,
      @BeanProperty var y: Float,
      @BeanProperty var z: Float)
    extends AbsMsg with _root_.adream_actions.PlaceRequest

  case class SRobot(
      @BeanProperty var label: String,
      @BeanProperty var room: String,
      @BeanProperty var position: _root_.adream_actions.Point,
      @BeanProperty var orientation: _root_.adream_actions.Quaternion)
    extends AbsMsg with _root_.adream_actions.Robot

  case class SSurfacesResponse(
      @BeanProperty var surfaces: java.util.List[_root_.adream_actions.Surface])
    extends AbsMsg with _root_.adream_actions.SurfacesResponse

  case class SPlaceResponse(
      @BeanProperty var status: Int)
    extends AbsMsg with _root_.adream_actions.PlaceResponse

  case class SPoint(
      @BeanProperty var x: Double,
      @BeanProperty var y: Double,
      @BeanProperty var z: Double)
    extends AbsMsg with _root_.adream_actions.Point

  case class SRobotsRequest(
      @BeanProperty var unused: String)
    extends AbsMsg with _root_.adream_actions.RobotsRequest

  case class SPickRequest(
      @BeanProperty var label: String)
    extends AbsMsg with _root_.adream_actions.PickRequest}

package actionlib_msgs {
  case class SGoalStatus(
      @BeanProperty var goalId: _root_.actionlib_msgs.GoalID,
      @BeanProperty var status: Byte,
      @BeanProperty var text: String)
    extends AbsMsg with _root_.actionlib_msgs.GoalStatus

  case class SGoalID(
      @BeanProperty var stamp: org.ros.message.Time,
      @BeanProperty var id: String)
    extends AbsMsg with _root_.actionlib_msgs.GoalID

  case class SGoalStatusArray(
      @BeanProperty var header: _root_.std_msgs.Header,
      @BeanProperty var statusList: java.util.List[_root_.actionlib_msgs.GoalStatus])
    extends AbsMsg with _root_.actionlib_msgs.GoalStatusArray}

//package move_base_msgs {
//  case class SMoveBaseAction(
//      @BeanProperty var actionGoal: _root_.move_base_msgs.MoveBaseActionGoal,
//      @BeanProperty var actionResult: _root_.move_base_msgs.MoveBaseActionResult,
//      @BeanProperty var actionFeedback: _root_.move_base_msgs.MoveBaseActionFeedback)
//    extends AbsMsg with _root_.move_base_msgs.MoveBaseAction
//
//  case class SMoveBaseResult(
//      )
//    extends AbsMsg with _root_.move_base_msgs.MoveBaseResult
//
//  case class SMoveBaseActionResult(
//      @BeanProperty var header: _root_.std_msgs.Header,
//      @BeanProperty var status: _root_.actionlib_msgs.GoalStatus,
//      @BeanProperty var result: _root_.move_base_msgs.MoveBaseResult)
//    extends AbsMsg with _root_.move_base_msgs.MoveBaseActionResult
//
//  case class SMoveBaseGoal(
//      @BeanProperty var targetPose: _root_.geometry_msgs.PoseStamped)
//    extends AbsMsg with _root_.move_base_msgs.MoveBaseGoal
//
//  case class SMoveBaseActionFeedback(
//      @BeanProperty var header: _root_.std_msgs.Header,
//      @BeanProperty var status: _root_.actionlib_msgs.GoalStatus,
//      @BeanProperty var feedback: _root_.move_base_msgs.MoveBaseFeedback)
//    extends AbsMsg with _root_.move_base_msgs.MoveBaseActionFeedback
//
//  case class SMoveBaseActionGoal(
//      @BeanProperty var header: _root_.std_msgs.Header,
//      @BeanProperty var goalId: _root_.actionlib_msgs.GoalID,
//      @BeanProperty var goal: _root_.move_base_msgs.MoveBaseGoal)
//    extends AbsMsg with _root_.move_base_msgs.MoveBaseActionGoal
//
//  case class SMoveBaseFeedback(
//      @BeanProperty var basePosition: _root_.geometry_msgs.PoseStamped)
//    extends AbsMsg with _root_.move_base_msgs.MoveBaseFeedback}
//
