package planstack.constraints.stnu

import planstack.graph.core.LabeledEdge

class MMV[ID](val edg : EDG) extends ISTNU[ID] with EDGListener {



  def consistent: Boolean = ???

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end and after start.
   *
   * @return ID of the created time point
   */
  def addVar(): Int = ???

  def checkConsistency(): Boolean = ???

  def checkConsistencyFromScratch(): Boolean = ???

  /**
   * Write a dot serialisation of the graph to file
   * @param file
   */
  def writeToDotFile(file: String): Unit = ???

  /**
   * Returns the earliest start time of time point u with respect to the start time point of the STN
   * @param u
   * @return
   */
  def earliestStart(u: Int): Int = ???

  /**
   * Returns the latest start time of time point u with respect to the start TP of the STN
   * @param u
   * @return
   */
  def latestStart(u: Int): Int = ???

  /**
   * Return the number of time points in the STN
   * @return
   */
  def size: Int = ???

  def addRequirement(from: Int, to: Int, value: Int): Boolean = ???

  def addContingent(from: Int, to: Int, lb: Int, ub: Int): Boolean = ???

  def addConditional(from: Int, to: Int, on: Int, value: Int): Boolean = ???

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): ISTNU[ID] = ???

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to: Int, value: Int): Boolean = ???

  def edgeAdded(e: LabeledEdge[Int, STNULabel]): Unit = ???

  def cycleDetected(): Unit = ???

  def squeezingDetected(): Unit = ???

  /** Adds a constraint to the STN specifying that v - u <= w.
    * The constraint is associated with an ID than can be later used to remove the constraint.
    * @return True if the STN tightened due to this operation.
    */
  override def addConstraintWithID(u: Int, v: Int, w: Int, id: ID): Boolean = ???

  /** Removes all constraints that were recorded with the given ID */
  override def removeConstraintsWithID(id: ID): Boolean = ???
}
