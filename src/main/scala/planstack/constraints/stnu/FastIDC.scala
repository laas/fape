package planstack.constraints.stnu

class FastIDC extends ISTNU with EDG {


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

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): ISTNU = ???
}
