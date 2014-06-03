package planstack.constraints.stnu

import scala.collection.mutable.ListBuffer

class FastIDC extends ISTNU with EDG {

  val todo = ListBuffer[E]()

  {
    val myStart = addVar()
    val myEnd = addVar()
    assert(myStart == start)
    assert(myEnd == end)
  }

  private var isConsistent = true
  def consistent = {
    while(todo.nonEmpty)
      fastIDC(todo.remove(0))
    isConsistent
  }

  def checkConsistency(): Boolean = ???

  def checkConsistencyFromScratch(): Boolean = ???



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
  def size: Int = ???

  /**
   * Returns a complete clone of the STN.
   * @return
   */
  def cc(): ISTNU = ???

  def edgeAdded(e : E) {
    if(e.l.negative)
      ccgraph.addEdge(e.u, e.v)
    if(ccgraph.acyclic && !squeezed) {
      todo += e
    } else {
      isConsistent = false
    }
  }

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
      D1(e) :: D2(e) :: D3(e) :: D4(e) ::D5(e) :: D6(e) ::D7(e) :: D8(e) :: D9(e) :: Nil

    for((toAdd,toRemove) <- additionAndRemovals) {
      for(edge <- toAdd) {
        addEdge(edge)
      }
    }

    true
  }
}
