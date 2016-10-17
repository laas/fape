package fr.laas.fape.constraints.stn

import fr.laas.fape.constraints.stn.bellmanford.CoreSTNIncBellmanFord
import fr.laas.fape.constraints.stnu.ElemStatus
import fr.laas.fape.constraints.stnu.ElemStatus._
import planstack.graph.core.{LabeledDigraph, LabeledEdge}
import planstack.graph.printers.GraphDotPrinter
import planstack.structures.IList


/**
 *
 * @param g A directed simple graph with integer edge labels.
 * @param consistent True if the STN is consistent, false otherwise
 * @param emptySpots Event index that are not occupied (because a variable was removed)
 */
abstract class GenCoreSTN[ID](val g: LabeledDigraph[Int, Int],
                              protected var notIntegrated : List[LabeledEdge[Int,Int]],
                              var consistent : Boolean,
                              var emptySpots : Set[Int])
  extends CoreSTN[ID]
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
    *
    * @return
   */
  def addVarUnsafe() : Int = {
    if(emptySpots.isEmpty) {
      g.addVertex(g.numVertices)
    } else {
      val v = emptySpots.head
      emptySpots = emptySpots.tail
      v
    }
  }

  override def events : IList[Int] = new IList((0 until size).filter(emptySpots.contains(_)).toList)

  /**
   * Return the number of time points in the STN
    *
    * @return
   */
  def size = g.numVertices - emptySpots.size

  /**
   * Returns the weight of an edge in the STN. If no such edge is present, an infinite weight will be returned.
   * Note that constraints are _not_ necessarily tightened and that a stronger indirect constraint might exist.
    *
    * @param u
   * @param v
   * @return
   */
  protected def getWeight(u:Int, v:Int) = {
    val edges = g.edges(u,v)
    if(edges.isEmpty)
      Weight.InfWeight
    else // first edge _must_ be the one with the minimum weight
      new Weight(edges.head.l)
  }


  /**
   * Adds a constraint to the STN specifying that v - u <= w
   * If a stronger constraint is already present, the STN isn't modified
   *
   * If the STN was indeed updated, its consistency is set to false.
    *
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
    *
    * @return True id the resulting STN is consistent.
   */
  override def addConstraint(u:Int, v:Int, w:Int) : Boolean = {
    addConstraintFast(u, v, w, None)
    checkConsistency()
  }

  /** Adds a constraint to the STN specifying that v - u <= w.
    * The constraint is associated with an ID than can be later used to remove the constraint.
    *
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
    *
    * @param file
   */
  def writeToDotFile(file:String) { new GraphDotPrinter(g).print2Dot(file) }

  /**
   * Returns the earliest start time of time point u with respect to the start time point of the STN
    *
    * @param u
   * @return
   */
  def earliestStart(u:Int) : Int

  /**
   * Returns the latest start time of time point u with respect to the start TP of the STN
    *
    * @param u
   * @return
   */
  def latestStart(u:Int) : Int

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
  def isConstraintPossible(u:Int, v:Int, w:Int) : Boolean = {
    val tmpSTN = cc()
    tmpSTN.addConstraint(u, v, w)
    tmpSTN.consistent
  }


  /** Removes all constraints that were recorded with the given ID */
  override def removeConstraintsWithID(id: ID): Boolean = {
    // function matching edges with the given id
    def hasID(e:LabeledEdge[Int,Int]) = e match {
      case eID:LabeledEdgeWithID[Int,Int,ID] @unchecked => eID.id == id
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

  override def removeVar(tp:Int): Boolean = {
    // match all edges in/out of tp
    def isOnVar(e:LabeledEdge[Int,Int]) : Boolean = e.u == tp || e.v == tp

    g.deleteEdges(isOnVar)
    notIntegrated = notIntegrated.filter(e => !isOnVar(e))

    emptySpots = emptySpots + tp

    checkConsistencyFromScratch()
  }

  private def optID(e:LabeledEdge[Any,Any]) : Option[ID] = e match {
    case e: LabeledEdgeWithID[_,_,ID] => Some(e.id)
    case _ => None
  }
  override def constraints: IList[(Int, Int, Int, ElemStatus, Option[ID])] =
    new IList((notIntegrated ++ g.edges()).map(x => (x.u, x.v, x.l, CONTROLLABLE, optID(x))))

  /**
   * Returns a complete clone of the STN.
 *
    * @return
   */
  def cc(): GenCoreSTN[ID]

}

object GenCoreSTN {

  /**
   * Return a an instance of the default implementation of an STN which uses an Incremental Bellman-Ford
   * algorithm to check its consistency.
 *
   * @return
    */
  def apply[ID]() : GenCoreSTN[ID] = new CoreSTNIncBellmanFord[ID]()
}
