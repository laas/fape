package fr.laas.fape.constraints.stn

import fr.laas.fape.constraints.stnu.ElemStatus
import planstack.structures.IList

abstract class CoreSTN[ID] {

  /** Id of the Start time point. No time points in the STN should happen before this one. */
  def start = 0

  /** Id of the End time point. No time point in the STN should happen after this one. */
  def end = 1

  def consistent : Boolean

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end.
   *
   * @return ID of the created time point
   */
  def addVar() : Int

  /** Returns a collection of all time points in this STN */
  def events : IList[Int]

  def constraints : IList[(Int, Int, Int, ElemStatus, Option[ID])]

  /**
   * Return the number of time points in the STN
 *
   * @return
   */
  def size :Int


  /**
   * Adds a constraint to the STN specifying that v - u <= w.
   *
   * @param u
   * @param v
   * @param w
   * @return
   */
  def addConstraint(u:Int, v:Int, w:Int) : Boolean

  /** Adds a constraint to the STN specifying that v - u <= w.
    * The constraint is associated with an ID than can be later used to remove the constraint.
    *
    * @return True if the STN tightened due to this operation.
    */
  def addConstraintWithID(u:Int, v:Int, w:Int, id:ID) : Boolean

  def checkConsistency() : Boolean
  def checkConsistencyFromScratch() : Boolean

  /**
   * Enforces that the time point u must happens before time point v or at the same time
   *
   * Results in the addition of an edge from v to u with weight 0: (v, u, 0)
 *
   * @param u
   * @param v
   */
  final def enforceBefore(u:Int, v:Int) {
    addConstraint(v, u, 0)
  }

  final def enforceMinDelay(u:Int, v:Int, d:Int) { addConstraint(v, u, -d) }
  final def enforceMaxDelay(u:Int, v:Int, d:Int) { addConstraint(u, v, d) }
  final def enforceMinDelayWithID(u:Int, v:Int, d:Int, id:ID) { addConstraintWithID(v, u, -d, id) }
  final def enforceMaxDelayWithID(u:Int, v:Int, d:Int, id:ID) { addConstraintWithID(u, v, d, id) }

  /**
   * Enforces that the time point u must happens strictly before time point v
   *
   * Results in the addition of an edge from v to u with weight -1: (v, u, -1)
 *
   * @param u
   * @param v
   */
  final def enforceStrictlyBefore(u:Int, v:Int) {
    addConstraint(v, u, -1)
  }

  /**
   * Creates a constraint stipulating that v in [u+min, u+max]
 *
   * @param u
   * @param v
   * @param min
   * @param max
   */
  final def enforceInterval(u:Int, v:Int, min:Int, max:Int) {
    enforceMinDelay(u, v, min)
    enforceMaxDelay(u, v, max)
  }

  /**
   * Write a dot serialisation of the graph to file
 *
   * @param file
   */
  def writeToDotFile(file:String)

  /**
   * Returns the earliest start time of time point u with respect to the start time point of the STN
 *
   * @param u
   * @return
   */
  def earliestStart(u:Int) : Int

  /**
   * Returns the latest start time of time point u with respect to the start TP of the STN
 *
   * @param u
   * @return
   */
  def latestStart(u:Int) : Int

  /**
   * Makespan of the STN (ie the earliest start of End)
 *
   * @return
   */
  final def makespan = earliestStart(end)

  /**
   * Returns true if the STN resulting in the addition of the constraint v - u <= w is consistent.
   *
   * Note that the default implementation works by propagating constraints on a clone of the current STN.
 *
   * @param u
   * @param v
   * @param w
   * @return
   */
  def isConstraintPossible(u:Int, v:Int, w:Int) : Boolean


  final def canBeBefore(u:Int, v:Int) : Boolean = isConstraintPossible(v, u, 0)

  final def canBeStrictlyBefore(u:Int, v:Int) = isConstraintPossible(v, u, -1)

  final def isMinDelayPossible(u:Int, v:Int, d:Int) = isConstraintPossible(v, u, -d)

  final def isMaxDelayPossible(u:Int, v:Int, d:Int) = isConstraintPossible(u, v, d)

  /** Removes all constraints that were recorded with the given ID */
  def removeConstraintsWithID(id:ID) : Boolean

  /** Remove a variable and all constraints that were applied on it; */
  def removeVar(u:Int) : Boolean

  /**
   * Returns a complete clone of the STN.
 *
    * @return
   */
  def cc() : CoreSTN[ID]

}
