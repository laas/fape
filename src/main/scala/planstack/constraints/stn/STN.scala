package planstack.constraints.stn

import StnPredef._
import planstack.graph.printers.GraphDotPrinter
import planstack.graph.core.LabeledDigraph

/**
 *
 * @param g A directed simple graph with integer edge labels.
 * @param consistent True if the STN is consistent, false otherwise
 */
abstract class STN(val g : LabeledDigraph[Int,Int], var consistent : Boolean) extends ISTN {

  // Creates start and end time points if the STN is empty
  if(size == 0) {
    addVarUnsafe()
    addVarUnsafe()
    enforceBefore(start, end)
  }

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



  /**
   * WARNING: Removes constraint from network but do _not_ update the distances. Make sure to always
   * make a checkConsistencyFromScratch after that.
   * @param u
   * @param v
   */
  def removeConstraintUnsafe(u:Int,v:Int) {
    g.deleteEdges(u, v)
  }


  /**
   * Remove the edge (u,v) in the constraint graph. The edge (v,u) is not removed.
   * Performs a consistency check from scratch (expensive try to use removeCOnstraints if you are to remove
   * more than one constraint)
   * @param u
   * @param v
   * @return True if the STN is consistent after removal
   */
  def removeConstraint(u:Int, v:Int) : Boolean = {
    removeConstraintUnsafe(u, v)
    checkConsistencyFromScratch()
  }

  /**
   * For all pairs, remove the corresponding directed edge in the constraint graph. After all of every pair are removed,
   * a consistency check is performed from scratch.
   * @param edges
   * @return true if the STN is consistent after removal
   */
  def removeConstraints(edges:Pair[Int,Int]*) = {
    edges.foreach(e => removeConstraintUnsafe(e._1, e._2))
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
