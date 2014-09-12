package planstack.constraints.stnu

import planstack.graph.core.LabeledEdge

class MMV(val edg : EDG) extends ISTNU with EDGListener {



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
  def cc(): ISTNU = ???

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to: Int, value: Int): Boolean = ???

  def edgeAdded(e: LabeledEdge[Int, STNULabel]): Unit = ???

  def cycleDetected(): Unit = ???

  def squeezingDetected(): Unit = ???
}
