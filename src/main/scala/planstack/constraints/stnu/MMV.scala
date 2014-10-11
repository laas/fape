package planstack.constraints.stnu

import planstack.constraints.stnu.Controllability._
import planstack.constraints.stnu.ElemStatus.ElemStatus
import planstack.structures.IList
import planstack.structures.Converters._

class MMV[ID](protected[stnu] var edg : EDG[ID],
              private var modified : List[Edge[ID]],
              protected var _consistent : Boolean,
              protected[stnu] var allConstraints : List[Edge[ID]],
              protected[stnu] var dispatchableVars : Set[Int],
              protected[stnu] var contingentVars : Set[Int],
              protected[stnu] var emptySpots : Set[Int])
  extends ISTNU[ID] with EDGListener[ID]
{
  type E = Edge[ID]

  def this() = this(new EDG[ID](checkCycles = false), Nil, true, List(), Set(), Set(), Set())

  /** Create a new MMV with exactly the same vars and edges than the given one */
  def this(toCopy:MMV[ID]) =
    this(new EDG(toCopy.edg), toCopy.modified, toCopy._consistent, toCopy.allConstraints,
      toCopy.dispatchableVars, toCopy.contingentVars, toCopy.emptySpots)

  // record ourself as the listener of any event in the EDG
  assert(edg.listener == null, "Error: already a listener on this EDG")
  edg.listener = this

  // create start and end time points
  if(size == 0) {
    val myStart = addVar()
    val myEnd = addVar()
    assert(myStart == start)
    assert(myEnd == end)
  }

  def consistent = {
    checkConsistency()
  }

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end and after start.
   *
   * @return ID of the created time point
   */
  def addVar(): Int =  {
    val n = edg.addVar()
    if(n != start && n != end) {
      this.enforceBefore(start, n)
      this.enforceBefore(n, end)
    } else if(n == end) {
      enforceBefore(start, end)
    }
    enforceInterval(n, n, 0, 0)
    n
  }

  def checkConsistency(): Boolean = checkConsistencyFromScratch()

  def checkConsistencyFromScratch(): Boolean = {
    while(_consistent && modified.nonEmpty) {
      edg.apsp()
      var queue = modified
      modified = Nil
      while(_consistent && queue.nonEmpty) {
        val e = queue.head
        queue = queue.tail

        val additionAndRemovals : List[(List[E],List[E])] = edg.classicalDerivations(e)
        for((toAdd,toRemove) <- additionAndRemovals) {
          for (edge <- toAdd)
            edg.addEdge(edge)
        }
      }
    }
    _consistent
  }

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
  def earliestStart(u: Int): Int =
    -edg.requirements.edgeValue(u, start).value

  /**
   * Returns the latest start time of time point u with respect to the start TP of the STN
   * @param u
   * @return
   */
  def latestStart(u: Int): Int = {
    if(edg.requirements.edgeValue(start, u) == null)
      Int.MaxValue
    else
      edg.requirements.edgeValue(start, u).value
  }

  /**
   * Return the number of time points in the STN
   * @return
   */
  def size: Int = edg.size - emptySpots.size

    def addRequirement(from: Int, to: Int, value: Int): Boolean = {
    val e = new E(from, to, new Requirement[ID](value))
    allConstraints = e :: allConstraints
    edg.addEdge(e).nonEmpty
  }

  def addRequirementWithID(from:Int, to:Int, value:Int, id:ID) : Boolean = {
    val e = new E(from, to, new RequirementWithID[ID](value, id))
    allConstraints = e :: allConstraints
    edg.addEdge(e).nonEmpty
  }

  def addContingent(from: Int, to: Int, lb: Int, ub:Int): Boolean = {
    val added = edg.addContingent(from, to, ub, None) ++ edg.addContingent(to, from, -lb, None)
    allConstraints = allConstraints ++added
    added.nonEmpty
  }

  def addContingentWithID(from:Int, to:Int, lb:Int, ub:Int, id:ID) : Boolean = {
    val added = edg.addContingent(from, to, ub, Some(id)) ++ edg.addContingent(to, from, -lb, Some(id))
    allConstraints = allConstraints ++ added
    added.nonEmpty
  }

  override protected[stnu] def addContingent(from: Int, to: Int, d: Int): Unit =
    allConstraints = allConstraints ++ edg.addContingent(from, to, d, None)

  override protected[stnu] def addContingentWithID(from: Int, to: Int, d: Int, id: ID): Unit =
    allConstraints = allConstraints ++ edg.addContingent(from, to, d, Some(id))

  protected def addConditional(from: Int, to: Int, on: Int, value: Int): Boolean = {
    edg.addConditional(from, to, on, value).nonEmpty
  }


  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): MMV[ID] = new MMV[ID](this)

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to: Int, value: Int): Boolean =
    edg.hasRequirement(from, to, value)

  def edgeAdded(e: E): Unit = modified = e :: modified

  def cycleDetected(): Unit = throw new RuntimeException("EDG should not be looking for cycles")
    //assert(!_consistent, "Should have been already detected through inconsistency.")

  def squeezingDetected(): Unit = _consistent = false

  /** Removes all constraints that were recorded with the given ID */
  override def removeConstraintsWithID(id: ID): Boolean = {
    allConstraints = allConstraints.filter((e:E) => e.l.optID match {
      case Some(constraintID) => id != constraintID
      case None => true
    })
    val prevSize = edg.size

    // empty every thing
    edg = new EDG[ID](checkCycles = false)
    edg.listener = this

    _consistent = true
    modified = Nil

    for(i <- 0 until edg.size)
      edg.addVar()
    for(e <- allConstraints) {
      if(e.l.cont) edg.addContingent(e.u, e.v, e.l.value)
      else if(e.l.req) edg.addRequirement(e.u, e.v, e.l.value)
      else throw new RuntimeException("An edge was recorded that is neither contingent or a requirement")
    }
    checkConsistencyFromScratch()
  }

  override def inconsistencyDetected(): Unit = _consistent = false

  /** Remove a variable and all constraints that were applied on it; */
  override def removeVar(u: Int): Boolean = ???

  /** Returns a collection of all time points in this STN */
  override def events: IList[Int] = (0 until edg.size).filter(!emptySpots.contains(_))

  override def controllability: Controllability = DYNAMIC_CONTROLLABILITY

  /** Adds a controllable variable. Only those variable can be executed */
  override def addDispatchable(): Int = {
    val i = addVar()
    dispatchableVars = dispatchableVars + i
    i
  }

  /** Adds a contingent variable */
  override def addContingentVar(): Int = {
    val i = addVar()
    contingentVars = contingentVars + i
    i
  }

  override def constraints: IList[(Int, Int, Int, ElemStatus, Option[ID])] = ???

  override def isContingent(v: Int): Boolean = contingentVars.contains(v)

  /** Returns true if a variable is dispatchable */
  override def isDispatchable(v: Int): Boolean = dispatchableVars.contains(v)
}
