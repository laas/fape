package fr.laas.fape.constraints.stnu

import fr.laas.fape.constraints.stn.CoreSTN

trait CoreSTNU[ID] extends CoreSTN[ID] {

  /**
   * Return the number of time points in the STN
 *
   * @return
   */
  def size: Int

  def controllability : Controllability

  /** Adds a Dispatchable variable. Only those variable can be executed */
  def addDispatchable() : Int

  /** Adds a contingent variable */
  def addContingentVar() : Int

  def isContingent(v : Int) : Boolean

  /** Returns true if a variable is dispatchable */
  def isDispatchable(v : Int) : Boolean

  final def addConstraint(u: Int, v: Int, w: Int): Boolean =
    addRequirement(u, v, w)

  final def addConstraintWithID(from:Int, to:Int, value:Int, id:ID) : Boolean =
    addRequirementWithID(from, to, value, id)


  def addRequirement(from:Int, to:Int, value:Int) : Boolean
  def addRequirementWithID(from:Int, to:Int, value:Int, id:ID) : Boolean
  def addContingent(from:Int, to:Int, lb:Int, ub:Int) : Boolean
  def addContingentWithID(from:Int, to:Int, lb:Int, ub:Int, id:ID) : Boolean
  protected[stnu] def addContingent(from:Int, to:Int, d:Int)
  protected[stnu] def addContingentWithID(from:Int, to:Int, d:Int, id:ID)

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
  def isConstraintPossible(u: Int, v: Int, w: Int): Boolean = {
    val clone = this.cc()
    clone.addConstraint(u, v, w)
    clone.checkConsistency()
  }

  /**
   * Returns a complete clone of the STN.
 *
   * @return
   */
  def cc(): CoreSTNU[ID]

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to:Int, value:Int) : Boolean

  /** Returns Some((min, max)) if there is a contingent constraint from --[min,max]--> to.
    * Returns None otherwise.
    */
  def getContingentDelay(from:Int, to:Int) : Option[(Int,Int)]
}
