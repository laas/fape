package fr.laas.fape.constraints.stnu.nilsson

import fr.laas.fape.constraints.stnu.{Controllability, CoreSTNU, Edge, ElemStatus}
import Controllability._
import fr.laas.fape.constraints.stn.LabeledEdgeWithID
import planstack.graph.printers.NodeEdgePrinter
import planstack.structures.Converters._
import planstack.structures.IList

import scala.collection.mutable.ListBuffer

class FastIDC[ID](protected[stnu] var edg : EDG[ID],
                  protected[stnu] val todo : ListBuffer[Edge[ID]],
                  protected[stnu] var isConsistent : Boolean,
                  protected[stnu] var emptySpots : Set[Int],
                  protected[stnu] var allConstraints : List[Edge[ID]],
                  protected[stnu] var dispatchableVars : Set[Int],
                  protected[stnu] var contingentVars : Set[Int])
  extends CoreSTNU[ID] with EDGListener[ID]
{
  type E = Edge[ID]

  def this() = this(new EDG[ID](checkCycles = true), ListBuffer[Edge[ID]](), true, Set(), List(), Set(), Set())

  def this(toCopy : FastIDC[ID]) =
    this(new EDG(toCopy.edg), toCopy.todo.clone(), toCopy.consistent, toCopy.emptySpots,
      toCopy.allConstraints, toCopy.dispatchableVars, toCopy.contingentVars)

  def controllability = DYNAMIC_CONTROLLABILITY

  // record ourself as the listener of any event in the EDG
  assert(edg.listener == null, "Error: already a listener on this EDG")
  edg.listener = this

  if(size == 0) {
    val myStart = addVar()
    val myEnd = addVar()
    assert(myStart == start)
    assert(myEnd == end)
  }

  override def addDispatchable() = {
    val v = addVar()
    dispatchableVars = dispatchableVars + v
    v
  }

  override def addContingentVar() = {
    val v = addVar()
    contingentVars = contingentVars + v
    v
  }

  def consistent : Boolean = checkConsistency()


  def checkConsistency(): Boolean =  {
    while(isConsistent && todo.nonEmpty)
      isConsistent &= fastIDC(todo.remove(0))
    isConsistent
  }

  def checkConsistencyFromScratch(): Boolean = { todo ++= edg.allEdges ; checkConsistency() }

  /**
   * Write a dot serialisation of the graph to file
   * @param file
   */
  def writeToDotFile(file: String): Unit = edg.exportToDot(file, new NodeEdgePrinter[Int, STNULabel[ID], E])

  /**
   * Returns the earliest start time of time point u with respect to the start time point of the STN
   * This operation is O(V*E). Do not use in any time-critical part.
   */
  def earliestStart(u: Int): Int = -edg.minDist(u, start)

  /**
   * Returns the latest start time of time point u with respect to the start TP of the STN
   * This operation is O(V*E). Do not use in any time-critical part.
   */
  def latestStart(u: Int): Int = edg.minDist(start, u)

  /**
   * Return the number of time points in the STN
   * @return
   */
  def size: Int = edg.size

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): FastIDC[ID] = new FastIDC(this)


  def fastIDC(e : E) : Boolean = {
    if(e.u == e.v) { // this is a loop on one vertex
      if(e.l.cond) {}
//        throw new RuntimeException("Don't know what to do with this conditional loop")
      else if(e.l.positive)
        return true
      else if(e.l.negative)
        return false
    }
    val additionAndRemovals = edg.derivationsFastIDC(e)

    for((toAdd,toRemove) <- additionAndRemovals) {
      for(edge <- toAdd) {
        edg.addEdge(edge)
      }
    }

    isConsistent
/*
    if(todo.size == prevSize) {
      //assert(!edg.hasNegativeCycles && !edg.squeezed)
      true
    } else {
      !edg.hasNegativeCycles && !edg.squeezed
    }*/
  }

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


  def edgeAdded(e: E): Unit = {
    todo += e
    for(neighbour <- edg.edgesOn(e.u)) todo += neighbour
    for(neighbour <- edg.edgesOn(e.v)) todo += neighbour
  }

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to: Int, value: Int): Boolean = edg.hasRequirement(from, to, value)

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end and after start.
   *
   * @return ID of the created time point
   */
  def addVar(): Int = {
    val n = edg.addVar()
    if(n != start && n != end) {
      this.enforceBefore(start, n)
      this.enforceBefore(n, end)
    } else if(n == end) {
      enforceBefore(start, end)
    }
    n
  }

  def cycleDetected() { isConsistent = false }

  def squeezingDetected() { isConsistent = false }

  /** Removes all constraints that were recorded with the given ID */
  override def removeConstraintsWithID(id: ID): Boolean = {
    allConstraints = allConstraints.filter((e:E) => e.l.optID match {
      case Some(constraintID) => id != constraintID
      case None => true
    })
    val prevSize = edg.size

    // empty every thing
    edg = new EDG[ID](checkCycles = true)
    edg.listener = this

    isConsistent = true
    todo.clear()

    for(i <- 0 until edg.size)
      edg.addVar()
    for(e <- allConstraints) {
      if(e.l.cont) edg.addContingent(e.u, e.v, e.l.value)
      else if(e.l.req) edg.addRequirement(e.u, e.v, e.l.value)
      else throw new RuntimeException("An edge was recorded that is neither contingent or a requirement")
    }
    checkConsistencyFromScratch()
  }

  override def inconsistencyDetected(): Unit = isConsistent = false

  /** Returns a collection of all time points in this STN */
  override def events: IList[Int] = (0 until size).filter(!emptySpots.contains(_))

  /** Remove a variable and all constraints that were applied on it; */
  override def removeVar(u: Int): Boolean = ???


  private def optID(e:E) : Option[ID] = e match {
    case e:LabeledEdgeWithID[_,_,ID] => Some(e.id)
    case _ => None
  }
  override def constraints : IList[(Int, Int, Int, ElemStatus, Option[ID])] = allConstraints.map((e:E) => {
    if(e.l.cont) (e.u, e.v, e.l.value, ElemStatus.CONTINGENT, optID(e))
    else if(e.l.req) (e.u, e.v, e.l.value, ElemStatus.CONTROLLABLE, optID(e))
    else throw new RuntimeException("This constraints should not be recorded: "+e)
  })

  override def isContingent(v: Int): Boolean = contingentVars.contains(v) //TODO: should be ture iff incoming contingent edge

  /** Returns true if a variable is dispatchable */
  override def isDispatchable(v: Int): Boolean = dispatchableVars.contains(v)

  /** Returns Some((min, max)) if there is a contingent constraint from --[min,max]--> to.
    * Returns None otherwise.
    */
  override def getContingentDelay(from: Int, to: Int): Option[(Int, Int)] =
    (edg.contingents.edge(to, from), edg.contingents.edge(from, to)) match {
      case (Some(min), Some(max)) => Some((-min.l.value, max.l.value))
      case (None, None) => None
      case _ => throw new RuntimeException("This contingent constraint does not seem symmetrical.")
    }
}
