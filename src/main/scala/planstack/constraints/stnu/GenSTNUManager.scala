package planstack.constraints.stnu

import planstack.constraints.stn.GenSTNManager
import ElemStatus._

import planstack.constraints.stnu.Controllability._
import planstack.structures.IList

abstract class GenSTNUManager[TPRef,ID] extends GenSTNManager[TPRef,ID] {

  def enforceContingent(u:TPRef, v:TPRef, min:Int, max:Int)

  def enforceContingentWithID(u:TPRef, v:TPRef, min:Int, max:Int, id:ID)

  def addControllableTimePoint(tp : TPRef) : Int
  def addContingentTimePoint(tp : TPRef) : Int

  final def checksPseudoControllability : Boolean = controllability.ordinal() >= PSEUDO_CONTROLLABILITY.ordinal()
  final def checksDynamicControllability : Boolean = controllability.ordinal() >= DYNAMIC_CONTROLLABILITY.ordinal()

  def controllability : Controllability

  /** Makes an independent clone of this STN. */
  override def deepCopy(): GenSTNUManager[TPRef, ID]

  /** Returns a list of all timepoints in this STNU, associated with a flag giving its status
    * (contingent or controllable. */
  def timepoints : IList[(TPRef, ElemStatus)]

  /** Returns a list of all constraints that were added to the STNU.
    * Each constraint is associated with flaw to distinguish between contingent and controllable ones. */
  def constraints : IList[(TPRef, TPRef, Int, ElemStatus, Option[ID])]
}


