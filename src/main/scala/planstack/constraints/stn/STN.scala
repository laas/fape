package planstack.constraints.stn

import StnPredef._
import planstack.graph.printers.GraphDotPrinter
import planstack.graph.core.LabeledDigraph

/**
 *
 * @param g A directed simple graph with integer edge labels.
 * @param consistent True if the STN is consistent, false otherwise
 */
abstract class STN(val g : LabeledDigraph[Int,Int], var consistent : Boolean) {

  // Creates start and end time points if the STN is empty
  if(size == 0) {
    addVarUnsafe()
    addVarUnsafe()
    enforceBefore(start, end)
  }

  /** Id of the Start time point. No time points in the STN should happen before this one. */
  val start = 0

  /** Id of the End time point. No time point in the STN should happen after this one. */
  val end = 1

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end and after start.
   *
   * @return ID of the created time point
   */
  final def addVar() : Int = {
//    if(!consistent) println("Error: adding variable %d to inconsistent STN".format(g.numVertices))

    val id = addVarUnsafe()
    enforceBefore(start, id)
    enforceBefore(id, end)

    id
  }

  /**
   * Creates a new time point _without_ adding constraints regarding start and end.
   * Note that it might cause the consistency checking to fail if some parts of the network are not reachable from/to start/end.
   * @return
   */
  def addVarUnsafe() : Int = {
    g.addVertex(g.numVertices)
  }

  /**
   * Return the number of time points in the STN
   * @return
   */
  def size = g.numVertices

  /**
   * Returns the weight of an edge in the STN. If no such edge is present, an infinite weight will be returned.
   * Note that constraints are _not_ necessarily tightened and that a stronger indirect constraint might exist.
   * @param u
   * @param v
   * @return
   */
  protected def getWeight(u:Int, v:Int) = {
    val edges = g.edges(u,v)
    if(edges.length == 0)
      Weight.InfWeight
    else // first edge _must_ be the one with the minimum weight
      new Weight(edges.head.l)
  }


  /**
   * Adds a constraint to the STN specifying that v - u <= w
   * If a stronger constraint is already present, the STN isn't modified
   *
   * If the STN was indeed updated, its consistency is set to false.
   * @param u
   * @param v
   * @param w
   * @return true if the STN was updated
   */
  protected def addConstraintFast(u:Int, v:Int, w:Int) : Boolean = {
    if(!consistent) {
      println("Warning: adding constraint to inconsistent STN")
      return false
    }
    val oldW = getWeight(u, v)
    if(oldW > new Weight(w)) {
      g.addEdge(u, v, w)
      consistent = false
      return true
    } else {
      return false
    }
  }


  /**
   * Adds a constraint to the STN specifying that v - u <= w.
   *
   * @param u
   * @param v
   * @param w
   * @return
   */
  def addConstraint(u:Int, v:Int, w:Int) : Boolean = {
    addConstraintFast(u, v, w)
    checkConsistency()
  }
  
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
  def writeToDotFile(file:String) { new GraphDotPrinter(g).print2Dot(file) }

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
  def isConstraintPossible(u:Int, v:Int, w:Int) : Boolean = {
    val tmpSTN = cc()
    tmpSTN.addConstraint(u, v, w)
    tmpSTN.consistent
  }

  def canBeBefore(u:Int, v:Int) : Boolean = isConstraintPossible(v, u, 0)


  /**
   * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
   * Performs a consistency check from scratch (expensive try to use removeCOnstraints if you are to remove
   * more than one constraint)
   * @param u
   * @param v
   * @return True if the STN is consistent after removal
   */
  def removeConstraint(u:Int, v:Int) : Boolean = {
    g.deleteEdges(u, v)
    checkConsistencyFromScratch()
  }

  /**
   * For all pairs, remove the corresponding directed edge in the constraint graph. After all of every pair are removed,
   * a consistency check is performed from scratch.
   * @param edges
   * @return true if the STN is consistent after removal
   */
  def removeConstraints(edges:Pair[Int,Int]*) = {
    edges.foreach(e => g.deleteEdges(e._1, e._2))
    checkConsistencyFromScratch()
  }

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc() : STN

}

object STN {

  /**
   * Return a an instance of the default implementation of an STN which uses an Incremental Bellman-Ford
   * algorithm to check its consistency.
   * @return
   */
  def apply() : STN = new STNIncBellmanFord()
}