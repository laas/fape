package planstack.constraints.stnu

import scala.collection.mutable.ListBuffer
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter

class FastIDC(val edg : EDG, val todo : ListBuffer[E], protected var isConsistent : Boolean) extends ISTNU with EDGListener {

  def this() = this(new EDG, ListBuffer[E](), true)

  def this(toCopy : FastIDC) = this(new EDG(toCopy.edg), toCopy.todo.clone(), toCopy.consistent)


  edg.listener = this
  assert(edg.listener == this)

  if(size == 0) {
    val myStart = addVar()
    val myEnd = addVar()
    assert(myStart == start)
    assert(myEnd == end)
  }

  def consistent : Boolean = {
    while(isConsistent && todo.nonEmpty)
      isConsistent &= fastIDC(todo.remove(0))

    isConsistent
  }


  def checkConsistency(): Boolean = consistent

  def checkConsistencyFromScratch(): Boolean = { todo ++= edg.allEdges ; consistent }



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
  def cc(): FastIDC = new FastIDC(this)


  def fastIDC(e : E) : Boolean = {
    if(e.u == e.v) { // this is a loop on one vertex
      if(e.l.cond) {}
//        throw new RuntimeException("Don't know what to do with this conditional loop")
      else if(e.l.positive)
        return true
      else if(e.l.negative)
        return false
    }
    val additionAndRemovals : List[(List[E],List[E])]=
      edg.D1(e) :: edg.D2(e) :: edg.D3(e) :: edg.D4(e) ::edg.D5(e) :: edg.D6(e) ::edg.D7(e) :: edg.D8(e) :: edg.D9(e) :: Nil

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
    edg.addRequirement(from, to, value).nonEmpty
  }

  def addContingent(from: Int, to: Int, lb: Int, ub:Int): Boolean = {
    (edg.addContingent(from, to, ub) ++ edg.addContingent(to, from, -lb)).nonEmpty
  }

  def addConditional(from: Int, to: Int, on: Int, value: Int): Boolean = {
    edg.addConditional(from, to, on, value).nonEmpty
  }


  def edgeAdded(e: E): Unit = {
    todo += e
    for(neighbour <- edg.allEdges) {
      if(neighbour.u == e.u || neighbour.u == e.v || neighbour.v == e.u || neighbour.v == e.v)
        todo += neighbour
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
