package planstack.constraints.stnu

import planstack.constraints.stn.ISTN

trait ISTNU[ID] extends ISTN[ID] {

  /**
   * Return the number of time points in the STN
   * @return
   */
  def size: Int

  def addConstraint(u: Int, v: Int, w: Int): Boolean = addRequirement(u, v, w)

  def addRequirement(from:Int, to:Int, value:Int) : Boolean
  def addContingent(from:Int, to:Int, lb:Int, ub:Int) : Boolean
  def addConditional(from:Int, to:Int, on:Int, value:Int) : Boolean

  /**
   * Returns true if the STN resulting in the addition of the constraint v - u <= w is consistent.
   *
   * Note that the default implementation works by propagating constraints on a clone of the current STN.
   * @param u
   * @param v
   * @param w
   * @return
   */
  def isConstraintPossible(u: Int, v: Int, w: Int): Boolean = ???

  /**
   * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
   * Performs a consistency check from scratch (expensive try to use removeCOnstraints if you are to remove
   * more than one constraint)
   * @param u
   * @param v
   * @return True if the STN is consistent after removal
   */
  def removeConstraint(u: Int, v: Int): Boolean = ???

  /**
   * For all pairs, remove the corresponding directed edge in the constraint graph. After all of every pair are removed,
   * a consistency check is performed from scratch.
   * @param edges
   * @return true if the STN is consistent after removal
   */
  def removeConstraints(edges: (Int, Int)*): Boolean = ???

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): ISTNU[ID]

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to:Int, value:Int) : Boolean

}
