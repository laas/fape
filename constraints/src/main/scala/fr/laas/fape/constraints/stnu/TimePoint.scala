package fr.laas.fape.constraints.stnu

abstract class TimePoint[TPRef](val tp: TPRef) {
  def isDispatchable : Boolean
  def isVirtual : Boolean
  def isContingent : Boolean

  def refToReal : Option[(TPRef, Int)]
}

final class DispatchableTimePoint[TPRef](tp: TPRef) extends TimePoint(tp) {
  override def isDispatchable = true
  override def isVirtual = false
  override def isContingent = false

  override def refToReal: Option[(TPRef, Int)] = sys.error("Only for vitual timepoints")
}

final class ContingentTimePoint[TPRef](tp: TPRef) extends TimePoint(tp) {
  override def isDispatchable = false
  override def isVirtual = false
  override def isContingent = true

  override def refToReal: Option[(TPRef, Int)] = sys.error("Only for vitual timepoints")
}

final class VirtualTimePoint[TPRef](tp: TPRef, val refToReal: Option[(TPRef,Int)]) extends TimePoint(tp) {
  override def isDispatchable = false
  override def isVirtual = true
  override def isContingent = false
}

/** Timepoints representing the start and end of the stnu */
final class StructuralTimePoint[TPRef](tp: TPRef) extends TimePoint(tp) {
  override def isDispatchable: Boolean = false
  override def isContingent: Boolean = false
  override def isVirtual: Boolean = false

  override def refToReal: Option[(TPRef, Int)] = sys.error("Only for vitual timepoints")
}