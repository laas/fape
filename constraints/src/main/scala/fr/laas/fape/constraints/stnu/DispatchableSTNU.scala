package fr.laas.fape.constraints.stnu

import fr.laas.fape.constraints.stnu.nilsson.{EDG, FastIDC}
import planstack.graph.core.SimpleLabeledDigraph


import scala.collection.mutable.ListBuffer
import scala.util.Random

trait DispatchableSTNU[ID] extends CoreSTNU[ID] {


  /** Mark a controllable var as executed at a given time */
  def executeVar(n:Int, atTime:Int)

  /** Set the occurrence time of a contingent event to a given time */
  def occurred(n:Int, atTime:Int)

  def dispatchableEvents(atTime:Int) : Iterable[Int]

  def isExecuted(n:Int) : Boolean

  def isEnabled(n:Int) : Boolean

  def setExecuted(n:Int)

  def isLive(u:Int, currentTime:Int) : Boolean =
    earliestStart(u) <= currentTime

  def maxDelay(from:Int, to:Int) : Option[Int]
  def minDelay(from:Int, to:Int) : Option[Int]

}



class Dispatcher[ID](_edg : EDG[ID],
                     _todo : ListBuffer[Edge[ID]],
                     _isConsistent : Boolean,
                     _emptySpots : Set[Int],
                     _allConstraints : List[Edge[ID]],
                     _dispatchableVars : Set[Int],
                     _contingentVars : Set[Int],
                     protected var enabled : Set[Int],
                     protected var executed : Set[Int])
  extends FastIDC[ID](_edg, _todo, _isConsistent, _emptySpots, _allConstraints, _dispatchableVars, _contingentVars)
  with DispatchableSTNU[ID]
{
  def this() = this(new EDG[ID](checkCycles = false), ListBuffer[Edge[ID]](), true, Set(), List(), Set(), Set(), Set(), Set())

  def this(toCopy : Dispatcher[ID]) =
    this(new EDG(toCopy.edg), toCopy.todo.clone(), toCopy.consistent, toCopy.emptySpots,
      toCopy.allConstraints, toCopy.dispatchableVars, toCopy.contingentVars, toCopy.enabled, toCopy.executed)

  var apsp : SimpleLabeledDigraph[Int,Int] = null

  override def setExecuted(n:Int): Unit =
    executed = executed + n

  def executeVar(u:Int, atTime:Int): Unit = {
    assert(!isExecuted(u))
    assert(dispatchableVars.contains(u))
    assert(latestStart(u) >= atTime)
    assert(isEnabled(u))
    this.enforceInterval(start, u, atTime, atTime)
    setExecuted(u)
    enabled = enabled ++ events.filter(isEnabled(_)).asScala
    checkConsistency()
  }

  def occurred(u:Int, atTime:Int) = {
    assert(!isExecuted(u))
    assert(contingentVars.contains(u))
    assert(latestStart(u) >= atTime)
    assert(enabled.contains(u))
    this.removeContingentOn(u)
    this.enforceInterval(start, u, atTime, atTime)
    removeWaitsOn(u)
    setExecuted(u)
    enabled = enabled ++ events.filter(isEnabled(_)).asScala
    checkConsistency()
  }

  override def isExecuted(n:Int) = executed.contains(n)

  override def isEnabled(u:Int): Boolean = {
    if(isExecuted(u))
      return false

    for(e <- apsp.outEdges(u)) {
      if (e.v != u && e.l <= 0 && isContingent(e.v) && !executed.contains(e.v))
        return false
      if (e.l < 0 && dispatchableVars.contains(e.v) && !executed.contains(e.v))
        return false
    }

    for(e <- edg.conditionals.outEdges(u)) {
      if(!isExecuted(e.l.node))
        if(earliestStart(e.v) == Int.MaxValue || earliestStart(e.v) - e.l.value > earliestStart(u))
          return false
    }
    true
  }

  protected def removeContingentOn(n:Int): Unit = {
    edg.contingents.deleteEdges((e:E) => {
      e.v == n && e.l.positive || e.u == n && e.l.negative
    })
  }

  protected def removeWaitsOn(u:Int) = {
    edg.conditionals.deleteEdges((e:E) => e.l.cond && e.l.node == u)
  }

  def contingentEvents(t:Int) : Iterable[(Int,Int)] = {
    for(n <- contingentVars ;
        if !isExecuted(n) ;
        if isLive(n, t) ;
        if t >= latestStart(n) || Random.nextInt(99)<10
    ) yield (n, t)
  }

  override def checkConsistency() = {
    if(todo.nonEmpty) {
      super.checkConsistency()
      apsp = edg.apspGraph()
      isConsistent
    } else {
      super.checkConsistency()
    }
  }

  override def checkConsistencyFromScratch() = {
    super.checkConsistencyFromScratch()
    apsp = edg.apspGraph()
    isConsistent
  }

  def getExecuted = executed
  def getLive(t:Int) = events.filter(!executed.contains(_)).filter(isLive(_, t))
  def getEnabled = enabled

  override def dispatchableEvents(atTime:Int): Iterable[Int] = {
    checkConsistency()
    enabled ++= events.filter(isEnabled(_)).asScala
    val executables = dispatchableVars.filter(n =>
      enabled.contains(n) && !executed.contains(n) && isLive(n, atTime))
    executables
  }


  def dispatch(): Unit = {
    enabled = enabled + start
    var currentTIme = 0
    while(executed.size != size && currentTIme < 200) {
//      controllables.filter(!executed.contains(_)).foreach(enforceMinDelay(start,_,currentTIme))
      checkConsistency()
      assert(isConsistent)
//      println("t="+currentTIme)
//      printAll(currentTIme)

      enabled = enabled ++ events.filter(isEnabled(_)).asScala
      for((n,t)               <- contingentEvents(currentTIme)) {
        println("Contingent: "+n+" "+t)
        occurred(n, t)
      }


      for(n <- dispatchableEvents(currentTIme))
        executeVar(n, currentTIme)



      currentTIme += 1
    }
  }

  override def earliestStart(u: Int): Int =
    if(u == start) 0
    else -apsp.edge(u, start).get.l

  override def latestStart(u: Int): Int =
    if(u == start) 0
    else apsp.edge(start, u).get.l

  override def cc(): Dispatcher[ID] = new Dispatcher[ID](this)

  override def maxDelay(from: Int, to: Int): Option[Int] = apsp.edge(from, to) match {
    case Some(e) => Some(e.l)
    case None => None
  }

  override def minDelay(from: Int, to: Int): Option[Int] = apsp.edge(to, from) match {
    case Some(e) => Some(-e.l)
    case None => None
  }
}

object Main extends App {
  val idc = new Dispatcher[String]()

  val WifeStore = idc.addDispatchable()
  val StartDriving = idc.addContingentVar()
  val WifeHome = idc.addContingentVar()
  val StartCooking = idc.addDispatchable()
  val DinnerReady = idc.addContingentVar()
  val v1 = idc.addVar()
  val v2 = idc.addVar()

  idc.enforceInterval(v1,StartDriving, 0, 10)
  idc.enforceInterval(WifeStore, v2, -5, 5)


  idc.addContingent(WifeStore, StartDriving, 30, 60)

  idc.addContingent(StartDriving, WifeHome, 35, 40)

  idc.addRequirement(WifeHome, DinnerReady, 5)

  idc.addRequirement(DinnerReady, WifeHome, 5)

//      idc.edg.exportToDot("before.dot", printer)

  idc.addContingent(StartCooking, DinnerReady, 25, 30)
  assert(idc.consistent)

//      idc.edg.exportToDot("incremental.dot", printer)
//      val full = idc.cc()
//      full.checkConsistencyFromScratch()
//      full.edg.exportToDot("full.dot", printer)

      // make sure the cooking starts at the right time
  assert(idc.hasRequirement(StartDriving, StartCooking, 10))
  assert(idc.hasRequirement(StartCooking, StartDriving, -10))

  idc.dispatch()
}