package fr.laas.fape.constraints.stnu

import fr.laas.fape.anml.model.concrete.{TPRef, TemporalConstraint}
import fr.laas.fape.constraints.stn.STN
import fr.laas.fape.structures.IList

trait STNU extends STN {

  def start : Option[TPRef]
  def end : Option[TPRef]

  def enforceContingent(u:TPRef, v:TPRef, min:Int, max:Int)

  def recordTimePoint(tp: TPRef): Int

  def removeTimePoint(tp: TPRef): Unit = ???

  /** Set the distance from the global start of the STN to tp to time */
  override def setTime(tp: TPRef, time: Int): Unit = {
    assert(start.nonEmpty, "This stn has no recorded start time point.")
    addConstraint(start.get, tp, time)
    addConstraint(tp, start.get, -time)
  }

  /** Record this time point as the global start of the STN */
  def recordTimePointAsStart(tp: TPRef): Int

  /** Unifies this time point with the global end of the STN */
  def recordTimePointAsEnd(tp: TPRef): Int

  def checksPseudoControllability : Boolean
  def checksDynamicControllability : Boolean

  /** If there is a contingent constraint [min, max] between those two timepoints, it returns
    * Some((min, max).
    * Otherwise, None is returned.
    */
  def contingentDelay(from:TPRef, to:TPRef) : Option[(Integer, Integer)]

  def controllability : Controllability

  /** Makes an independent clone of this STNU. */
  override def deepCopy(): STNU

  /** Returns a list of all timepoints in this STNU, associated with a flag giving its status
    * (contingent or controllable. */
  def timepoints : IList[TPRef]

  final def getEndTimePoint: Option[TPRef] = end

  final def getStartTimePoint: Option[TPRef] = start

  def getMinDelay(u:TPRef, v:TPRef) : Int
  def getMaxDelay(u: TPRef, v:TPRef) : Int

  /** Returns a list of constraints that do not involve any structural timepoints.
    * Constraints between structural timepoints must have been compiled as constraints
    * between non-structural timepoints. */
  def getConstraintsWithoutStructurals : IList[TemporalConstraint]

  /** Returns the list of all constraints that were added to the STNU. */
  def getOriginalConstraints : IList[TemporalConstraint]
}