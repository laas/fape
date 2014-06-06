package planstack.constraints.stnu

import scala.collection.mutable.ListBuffer
import planstack.graph.printers.NodeEdgePrinter
import planstack.graph.core.{LabeledEdge, LabeledDigraph, SimpleLabeledDigraph}
import planstack.graph.GraphFactory
import scala.collection.mutable


/*** NOT FINISHED. This implementation of EfficientIDC is not finished yet. */
class EfficientIDC(val edg : EDG, val todo : ListBuffer[Int]) extends ISTNU with EDGListener {

  def this() = this(new EDG, ListBuffer[Int]())

  def this(toCopy : EfficientIDC) = this(new EDG(toCopy.edg), toCopy.todo.clone())

  val ddg = GraphFactory.getMultiLabeledDigraph[Int, Int]


  edg.listener = this
  assert(edg.listener == this)

  if(size == 0) {
    val myStart = addVar()
    val myEnd = addVar()
    assert(myStart == start)
    assert(myEnd == end)
  }

  private var isConsistent = true
  def consistent = {
    while(isConsistent && todo.nonEmpty) {
      efficientIDC(todo.remove(0))
    }

    isConsistent
  }


  def checkConsistency(): Boolean = consistent

  def checkConsistencyFromScratch(): Boolean = ???



  /**
   * Write a dot serialisation of the graph to file
   * @param file
   */
  def writeToDotFile(file: String): Unit = edg.exportToDot(file, new NodeEdgePrinter[Int, STNULabel, E])

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
  def size: Int = edg.size

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): EfficientIDC = new EfficientIDC(this)

  def addRequirement(from: Int, to: Int, value: Int): Boolean = {
    edg.addRequirement(from, to, value).nonEmpty
  }

  def addContingent(from: Int, to: Int, lb: Int, ub:Int): Boolean = {
    (edg.addContingent(from, to, ub) ++ edg.addContingent(to, from, -lb)).nonEmpty
  }

  def addConditional(from: Int, to: Int, on: Int, value: Int): Boolean = {
    edg.addConditional(from, to, on, value).nonEmpty
  }


  def edgeAdded(e: E): Unit = {
    // TODO check if target != current
    todo += e.v
    if(e.l.negative)
      todo += e.u

    if(e.l.posReq) {
      ddg.addEdge(e.v, e.u, e.l.value)
    } else if(e.l.cont && e.l.req) {
      ddg.addEdge(e.u, e.v, -e.l.value)
    } else if(e.l.negReq && ddg.edges(e.v, e.u).nonEmpty) {
      // this negative requirement potentially removed an positive requirement from the EDG
      // remove those edges from the ddg and reinsert the one issued by a contingent edge
      ddg.deleteEdges(e.v, e.u)
      edg.contingents.edges(e.v, e.u)
        .filter(contingent => contingent.l.negative)
        .foreach(contingent => ddg.addEdge(contingent.u, contingent.v, - contingent.l.value))
    }
  }

  def efficientIDC(current : Int) {
    processCond(current, ddg.cc)
    processNegReq(current, ddg.cc)
    processPosReq(current)
  }



  def processCond(current : Int, ddg : LabeledDigraph[Int, Int]) {
    val allCond = edg.inConditionals(current)
    val condNodes = allCond.map(_.l.node).toSet

    for(c <- condNodes) {
      val edges = allCond.filter(_.l.node == c)
      // absolute value of the most negative edge
      val minW = edges.foldLeft(0)((min, conditional) =>
        if(conditional.l.value < min) conditional.l.value
        else min).abs

      for(e <- edges) {
        ddg.addEdge(e.v, e.u, e.l.value + minW)
      }
      for((node, cost) <- EfficientIDC.limitedDijkstra(current, ddg, minW) ; if node != current) {
        val e = new E(node, current, new Conditional(c, cost - minW))
        if(edg.tightens(e)) {
          edg.addEdge(e);
          {
            val (toAdd, toRemove) = edg.D8(e)
            for(addition <- toAdd) edg.addEdge(addition)
            for(removal <- toRemove) { /***** TODO ****/ }
          }; {
            val (toAdd, toRemove) = edg.D9(e)
            for(addition <- toAdd) edg.addEdge(addition)
            for(removal <- toRemove) { /***** TODO ****/ }
          }
        }
      }
    }
  }

  def processNegReq(current : Int, ddg : LabeledDigraph[Int, Int]) {
    val edges = edg.inNegReq(current)
    val minW = edges.foldLeft(0)((min, negReq) =>
      if(negReq.l.value < min) negReq.l.value
      else min
    ).abs

    for(e <- edges) {
      ddg.addEdge(e.v, e.u, e.l.value + minW)
    }
    for((node, cost) <- EfficientIDC.limitedDijkstra(current, ddg, minW) ; if node != current) {
      val e = new E(node, current, new Requirement(cost - minW))
      edg.addEdge(e)
    }
  }

  def processPosReq(current : Int) {
    val edges = edg.inPosReq(current)
    for(e <- edges) {
      val additionAndRemovals : List[(List[E],List[E])] = edg.D1(e) :: edg.D4(e) ::edg.D5(e) :: Nil

      for((toAdd,toRemove) <- additionAndRemovals) {
        for(edge <- toAdd) {
          edg.addEdge(edge)
        }
        //TODO : toRemove
      }
    }
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
    ddg.addVertex(n)
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
}

object EfficientIDC {

  def limitedDijkstra(start:Int, ddg:LabeledDigraph[Int,Int], maxCost:Int) : Seq[(Int, Int)] = {
    val results = ListBuffer[(Int,Int)]()
    val marked = mutable.Set[Int]()
    val queue = new mutable.PriorityQueue[(Int,Int)]()(new Ordering[(Int,Int)] {
      def compare(x: (Int, Int), y: (Int, Int)): Int = y._2 - x._2
    })
    queue += ((start, 0))

    while(!queue.isEmpty) {
      val label = queue.dequeue()
      if(!marked.contains(label._1)) {
        marked += label._1
        results += label
        for(e <- ddg.outEdges(label._1) ; if label._2 + e.l <= maxCost) {
          queue += ((e.v, label._2 + e.l))
        }
      }
    }

    results
  }
}