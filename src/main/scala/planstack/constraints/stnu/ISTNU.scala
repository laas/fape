package planstack.constraints.stnu

import planstack.constraints.stn.ISTN

trait ISTNU[ID] extends ISTN[ID] {

  /**
   * Return the number of time points in the STN
   * @return
   */
  def size: Int

  final def addConstraint(u: Int, v: Int, w: Int): Boolean =
    addRequirement(u, v, w)

  final def addConstraintWithID(from:Int, to:Int, value:Int, id:ID) : Boolean =
    addRequirementWithID(from, to, value, id)


  def addRequirement(from:Int, to:Int, value:Int) : Boolean
  def addRequirementWithID(from:Int, to:Int, value:Int, id:ID) : Boolean
  def addContingent(from:Int, to:Int, lb:Int, ub:Int) : Boolean
  def addContingentWithID(from:Int, to:Int, lb:Int, ub:Int, id:ID) : Boolean

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
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): ISTNU[ID]

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to:Int, value:Int) : Boolean

}
