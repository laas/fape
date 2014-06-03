package planstack.constraints.stn

trait ISTN {
  /** Id of the Start time point. No time points in the STN should happen before this one. */
  val start = 0

  /** Id of the End time point. No time point in the STN should happen after this one. */
  val end = 1

  def consistent : Boolean

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end and after start.
   *
   * @return ID of the created time point
   */
  def addVar() : Int

  /**
   * Return the number of time points in the STN
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

  def checkConsistency() : Boolean
  def checkConsistencyFromScratch() : Boolean

  /**
   * Enforces that the time point u must happens before time point v or at the same time
   *
   * Results in the addition of an edge from v to u with weight 0: (v, u, 0)
   * @param u
   * @param v
   */
  def enforceBefore(u:Int, v:Int) {
    addConstraint(v, u, 0)
  }

  /**
   * Enforces that the time point u must happens strictly before time point v
   *
   * Results in the addition of an edge from v to u with weight -1: (v, u, -1)
   * @param u
   * @param v
   */
  def enforceStrictlyBefore(u:Int, v:Int) {
    addConstraint(v, u, -1)
  }

  /**
   * Creates a constraint stipulating that v in [u+min, u+max]
   * @param u
   * @param v
   * @param min
   * @param max
   */
  def enforceInterval(u:Int, v:Int, min:Int, max:Int) {
    addConstraint(u, v, max)
    addConstraint(v, u, -min)
  }

  /**
   * Write a dot serialisation of the graph to file
   * @param file
   */
  def writeToDotFile(file:String)

  /**
   * Returns the earliest start time of time point u with respect to the start time point of the STN
   * @param u
   * @return
   */
  def earliestStart(u:Int) : Int

  /**
   * Returns the latest start time of time point u with respect to the start TP of the STN
   * @param u
   * @return
   */
  def latestStart(u:Int) : Int

  /**
   * Makespan of the STN (ie the earliest start of End)
   * @return
   */
  def makespan = earliestStart(end)

  /**
   * Returns true if the STN resulting in the addition of the constraint v - u <= w is consistent.
   *
   * Note that the default implementation works by propagating constraints on a clone of the current STN.
   * @param u
   * @param v
   * @param w
   * @return
   */
  def isConstraintPossible(u:Int, v:Int, w:Int) : Boolean


  def canBeBefore(u:Int, v:Int) : Boolean = isConstraintPossible(v, u, 0)


  /**
   * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
   * Performs a consistency check from scratch (expensive try to use removeCOnstraints if you are to remove
   * more than one constraint)
   * @param u
   * @param v
   * @return True if the STN is consistent after removal
   */
  def removeConstraint(u:Int, v:Int) : Boolean

  /**
   * For all pairs, remove the corresponding directed edge in the constraint graph. After all of every pair are removed,
   * a consistency check is performed from scratch.
   * @param edges
   * @return true if the STN is consistent after removal
   */
  def removeConstraints(edges:Pair[Int,Int]*) :Boolean

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc() : ISTN

}
