package planstack.constraints.stnu

/** Status applicable to timepoint and constraints that mark them
  * as Controllable, Contingent.
  */
object ElemStatus extends Enumeration {
  type ElemStatus = Value

  /** Time points that are controllable but will not be dispatched */
  val NO_FLAG = Value
  val START = Value
  val END = Value
  val CONTROLLABLE = Value
  val CONTINGENT = Value

}
