package planstack.constraints.stnu

import planstack.UniquelyIdentified
import planstack.anml.model.concrete.TPRef
import planstack.graph.GraphFactory
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.{GraphDotPrinter, NodeEdgePrinter}
import planstack.structures.IList

import scala.language.implicitConversions
import ElemStatus._
import planstack.structures.Converters._

class STNUDispatcher[ID](from:GenSTNUManager[ID]) {
  val dispatcher :DispatchableSTNU[ID] = new Dispatcher[ID]

  /** Timepoints that must stay in the STN (dispatchable, contingents and stn's start end */
  val fixedTps =
    (for((tp, flag) <- from.timepoints
         if flag != ElemStatus.NO_FLAG && flag != ElemStatus.RIGID) yield
      tp
    ).toSet

  /** Graph with edges from the fixed timepoints to the rigid ones */
  val g = getRigidRelationsGraph()

  /** rigids timepoints are the one in a rigid relation with the fixed ones. */
  val rigids = g.vertices.filter(!fixedTps.contains(_))

  /** Other timepoints: neither rigid nor fixed, those are controllable but not actions start (not explicitly
    * flagged as dispatchable) */
  val others = from.timepoints.map(_._1).filter(!g.vertices.contains(_))


  /**
   * If one/two timepoints of the constraint are in a rigid constraint with a fixed one,
   * the constraint is modified to be on the fixed timepoints.
   */
  private def finalConstraint(c : Constraint[ID]): Constraint[ID] = {
    // given tp, if there is a rigid constraint x -- d --> tp,
    // it returns (x, d).
    // otherwise, it returns (tp, 0)
    def distToRigid(tp:TPRef, initDist : Int = 0): (TPRef, Int) = {
      if(fixedTps.contains(tp) || others.contains(tp))
        (tp, initDist)
      else {
        val e = g.inEdges(tp).head
        distToRigid(e.u, initDist + e.l)
      }
    }

    // we have a constraint a -- x --> b where a and b have
    // other rigid constraints tp1 -- d1 --> a  and  tp2 -- d2 --> b
    // it is replaced with tp1 -- d1 + x - d2 --> tp2
    // note that we could have (result of dist) tp1 = a and d1 =0 or
    // tp2=b and d2=0 which does not change the validity
    val (tp1, d1) = distToRigid(c.u)
    val (tp2, d2) = distToRigid(c.v)

    new Constraint[ID](tp1, tp2, d1 + c.d - d2, c.tipe, c.optID)
//    (tp1, tp2, d1 + c._3 - d2, c._4, c._5)
  }

  val finalConstraints = from.constraints.map(finalConstraint(_))

  /** For all non-rigid timepoints, they are added to the STN */
  val ids : Map[TPRef,Int] =
    (for((tp, flag) <- from.timepoints ; if !rigids.contains(tp)) yield flag match {
      case CONTINGENT => (tp, dispatcher.addContingentVar())
      case CONTROLLABLE => (tp, dispatcher.addDispatchable())
      case START => (tp, dispatcher.start)
      case END => (tp, dispatcher.end)
      case _ => (tp, dispatcher.addVar()) // other time points that were not rigidly constrained
    }).toMap

  val tps = ids.map(_.swap)

  implicit def tp2id(tp:TPRef) : Int = ids(tp)
  implicit def id2tp(id:Int) : TPRef = tps(id)


  for(c <- from.constraints ; if c.tipe == CONTINGENT) {
    c.optID match {
      case Some(id) => dispatcher.addContingentWithID(c.u, c.v, c.d, id)
      case None => dispatcher.addContingent(c.u, c.v, c.d)
    }
  }

  for(c <- finalConstraints ; if c.tipe == ElemStatus.CONTROLLABLE) {
    c.optID match {
      case Some(id) => dispatcher.addConstraintWithID(c.u, c.v, c.d, id)
      case None => dispatcher.addConstraint(c.u, c.v, c.d)
    }
  }


  /** Builds a graph with edges from fixed to rigid or rigid to rigid.
    * The distance is the one of the rigid constraint between the two timepoints.
    *
    * This is currently the bottleneck, mainly because of the simple digraph where edges are filtered out
    * whenever a new one is added.
    */
  private def getRigidRelationsGraph() = {
    var rigids = fixedTps ++ from.timepoints.filter(_._2 == RIGID).map(_._1)
    var addedToRigids = true

    val rigidsRelations = GraphFactory.getSimpleLabeledDigraph[TPRef, Int]
    for(tp <- rigids) rigidsRelations.addVertex(tp)
    for(c <- from.constraints if c.tipe == RIGID) {
      assert(fixedTps.contains(c.u) || rigids.contains(c.u)) //TODO: is it really valid ?
      assert(rigids.contains(c.v))
      rigidsRelations.addEdge(c.u, c.v, c.d)
    }

    while (addedToRigids) {
      addedToRigids = false
      val g = GraphFactory.getSimpleLabeledDigraph[TPRef, Int]()
      var dist = Map[TPRef, Map[TPRef, Int]]()

      for (tp <- fixedTps)
        g.addVertex(tp)

      for (c <- from.constraints
           if c.tipe != RIGID // was already added
           if rigids.contains(c.u) && !rigids.contains(c.v)
             || !rigids.contains(c.u) && rigids.contains(c.v)) {

        if (!g.contains(c.u)) g.addVertex(c.u)
        if (!g.contains(c.v)) g.addVertex(c.v)

        g.edge(c.u, c.v) match {
          case Some(e) if e.l < c.d => // edge is already stronger
          case _ => g.addEdge(c.u, c.v, c.d)
        }

        g.edge(c.v, c.u) match {
          case Some(e) if e.l == -c.d => {
            rigids = rigids + c.u + c.v
            addedToRigids = true
            if (rigidsRelations.contains(c.u)) {
              rigidsRelations.addVertex(c.v)
              rigidsRelations.addEdge(c.u, c.v, c.d)
            } else {
              assert(rigidsRelations.contains(c.v))
              rigidsRelations.addVertex(c.u)
              rigidsRelations.addEdge(c.v, c.u, -c.d)
            }
          }
          case _ =>
        }
      }
    }
    rigidsRelations
  }

  dispatcher.checkConsistencyFromScratch()


  def isConsistent = dispatcher.checkConsistency()

  def setHappened(tp:TPRef): Unit = {
    dispatcher.setExecuted(tp)
  }

  def getDispatchable(time:Int) : IList[TPRef] = {
    val t:Integer = time
    new IList[TPRef](dispatcher.dispatchableEvents(time).map(id => id2tp(id)).toList)
  }

  def isEnabled(tp: TPRef) : Boolean = {
    dispatcher.isEnabled(tp)
  }

  def getMaxDelay(from:TPRef, to:TPRef) : Int = {
    dispatcher.maxDelay(from, to) match {
      case Some(e) => e
      case None => throw new RuntimeException("No contingent delay between those two time points")
    }
  }

  def getMinDelay(from:TPRef, to:TPRef) : Int = {
    dispatcher.minDelay(from, to) match {
      case Some(e) => e
      case None => throw new RuntimeException("No contingent delay between those two time points")
    }
  }

  /** A debugging output that shows all timepoints and the category they were put in. */
  def print(printer : NodeEdgePrinter[TPRef, Object, planstack.graph.core.Edge[TPRef]]): Unit = {
    val p = new GraphDotPrinter[TPRef,Int,LabeledEdge[TPRef,Int]](g, printer.printNode, x => x.toString, _ => false, _ => false)
    p.print2Dot("rigid-relations.dot")

    val others = from.timepoints.map(_._1).filter(!g.vertices.contains(_))
    val rigid = g.vertices.filter(!fixedTps.contains(_))

    println("\nFixed")
    for(tp <- fixedTps) println(printer.printNode(tp))

    println("\nRigid")
    for(tp <- rigid) println(printer.printNode(tp))

    println("\nOthers")
    for(tp <- others) println(printer.printNode(tp))

    println("%s  %s  %s".format(fixedTps.size, rigid.size, others.size))
  }

}
