package planstack.constraints.stn

import planstack.graph.core.{LabeledDigraph, LabeledEdge}
import planstack.graph.printers.GraphDotPrinter

/**
 *
 * @param g A directed simple graph with integer edge labels.
 * @param consistent True if the STN is consistent, false otherwise
 *
 */
abstract class STN[ID](
                        val g : LabeledDigraph[Int,Int],
                        protected var notIntegrated : List[LabeledEdge[Int,Int]],
                        var consistent : Boolean)
  extends ISTN[ID]
{

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
  protected def addConstraintFast(u:Int, v:Int, w:Int, optID : Option[ID]) : Boolean = {
    if(!consistent) {
      return false
    }
    val e = optID match {
      case Some(id) => new LabeledEdgeWithID(u,v,w,id)
      case None => new LabeledEdge(u,v,w)
    }

    if(getWeight(u, v) > new Weight(w)) {
      g.addEdge(e)
      consistent = false
      true
    } else {
      notIntegrated = e :: notIntegrated
      false
    }
  }


  /**
   * Adds a constraint to the STN specifying that v - u <= w.
   * @return True id the resulting STN is consistent.
   */
  override def addConstraint(u:Int, v:Int, w:Int) : Boolean = {
    addConstraintFast(u, v, w, None)
    checkConsistency()
  }

  /** Adds a constraint to the STN specifying that v - u <= w.
    * The constraint is associated with an ID than can be later used to remove the constraint.
    * @return True if the STN tightened due to this operation.
    */
  override def addConstraintWithID(u:Int, v:Int, w:Int, id:ID) : Boolean = {
    addConstraintFast(u, v, w, Some(id))
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
  @Deprecated
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
  @Deprecated
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
  @Deprecated
  def removeConstraints(edges:Pair[Int,Int]*) = {
    edges.foreach(e => removeConstraintUnsafe(e._1, e._2))
    checkConsistencyFromScratch()
  }


  /** Removes all constraints that were recorded with the given ID */
  override def removeConstraintsWithID(id: ID): Boolean = {
    val e = new LabeledEdgeWithID(1,1,1,"st")

    // function matching edges with the given id
    def hasID(e:LabeledEdge[Int,Int]) = e match {
      case eID:LabeledEdgeWithID[Int,Int,ID] => eID.id == id
      case _ => false
    }

    // remove those constraints from the graph
    g.deleteEdges(hasID)

    // remove those constraints from the not integrated list
    notIntegrated = notIntegrated.filter(e => !hasID(e))

    // make sure we have integrated all constraint that are now relevant
    for(e <- notIntegrated.reverse)
      if(getWeight(e.u, e.v) > e.l)
        g.addEdge(e)

    checkConsistencyFromScratch()
  }

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc() : STN[ID]

}

object STN {

  /**
   * Return a an instance of the default implementation of an STN which uses an Incremental Bellman-Ford
   * algorithm to check its consistency.
   * @return
   */
  def apply[ID]() : STN[ID] = new STNIncBellmanFord[ID]()
}
