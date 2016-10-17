package fr.laas.fape.constraints.stnu

import Controllability._
import fr.laas.fape.constraints.stn.LabeledEdgeWithID
import fr.laas.fape.constraints.stnu.nilsson._
import ElemStatus._
import planstack.graph.core.LabeledEdge
import planstack.graph.core.impl.intindexed.{DirectedMultiLabeledIIAdjList, DirectedSimpleLabeledIIAdjList}
import planstack.graph.core.impl.matrix.FullIntIntDigraph
import planstack.structures.Converters._
import planstack.structures.IList

class MMV[ID](var contingents : DirectedSimpleLabeledIIAdjList[Contingent[ID]],
              var requirements : FullIntIntDigraph,
              var conditionals : DirectedMultiLabeledIIAdjList[Conditional[ID]],
              private var modified : List[Edge[ID]],
              protected var _consistent : Boolean,
              protected[stnu] var allConstraints : List[Edge[ID]],
              protected[stnu] var dispatchableVars : Set[Int],
              protected[stnu] var contingentVars : Set[Int],
              protected[stnu] var emptySpots : Set[Int],
              protected[stnu] var enabled : Set[Int],
              protected[stnu] var executed : Set[Int])
  extends CoreSTNU[ID]
  with DispatchableSTNU[ID]
{
  type E = Edge[ID]

  // if set to true, IFPC will be invoked every time a requirement is explicitly added
  private var enabledIFPC = true

  def this() = this(new DirectedSimpleLabeledIIAdjList[Contingent[ID]](),
    new FullIntIntDigraph(Int.MaxValue),
    new DirectedMultiLabeledIIAdjList[Conditional[ID]](),
    Nil, true, List(), Set(), Set(), Set(), Set(), Set())

  /** Create a new MMV with exactly the same vars and edges than the given one */
  def this(toCopy:MMV[ID]) =
    this(toCopy.contingents.cc(),toCopy.requirements.cc(), toCopy.conditionals.cc(),
      toCopy.modified, toCopy._consistent, toCopy.allConstraints,
      toCopy.dispatchableVars, toCopy.contingentVars, toCopy.emptySpots, toCopy.enabled, toCopy.executed)

  // create start and end time points
  if(size == 0) {
    val myStart = addVar()
    val myEnd = addVar()
    assert(myStart == start)
    assert(myEnd == end)
  }

  private final def dist(i:Int, j:Int) = requirements.edgeValue(i, j)

  def consistent = {
    checkConsistency()
  }

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end and after start.
   *
   * @return ID of the created time point
   */
  def addVar(): Int =  {
    requirements.addVertex()
    conditionals.addVertex()
    val n = contingents.addVertex()
    if(n != start && n != end) {
      this.enforceBefore(start, n)
      this.enforceBefore(n, end)
    } else if(n == end) {
      enforceBefore(start, end)
    }
    enforceInterval(n, n, 0, 0)
    n
  }

  def checkConsistency(): Boolean = {
    while(_consistent && modified.nonEmpty) {
      val e = modified.head
      modified = modified.tail

      val additionAndRemovals : List[(List[E],List[E])] = classicalDerivations(e)
        for((toAdd,toRemove) <- additionAndRemovals) {
          for (edge <- toAdd)
            addEdge(edge)
        }
    }
    _consistent
  }

  def checkConsistencyFromScratch(): Boolean = {
    modified = Nil
    modified = modified ++ requirements.edges().map(e => new E(e.u, e.v, new Requirement[ID](e.l)))
    modified = modified ++ contingents.edges()
    modified = modified ++ conditionals.edges()
    enabledIFPC = false
    while(_consistent && modified.nonEmpty) {
      apsp()
      var queue = modified
      modified = Nil
      while(_consistent && queue.nonEmpty) {
        val e = queue.head
        queue = queue.tail

        val additionAndRemovals : List[(List[E],List[E])] = classicalDerivations(e)
        for((toAdd,toRemove) <- additionAndRemovals) {
          for (edge <- toAdd)
            addEdge(edge)
        }
      }
    }
    enabledIFPC = true
    _consistent
  }

  /**
   * Write a dot serialisation of the graph to file
 *
   * @param file
   */
  def writeToDotFile(file: String): Unit = ???

  /**
   * Returns the earliest start time of time point u with respect to the start time point of the STN
 *
   * @param u
   * @return
   */
  def earliestStart(u: Int): Int =
    -requirements.edgeValue(u, start)

  /**
   * Returns the latest start time of time point u with respect to the start TP of the STN
 *
   * @param u
   * @return
   */
  def latestStart(u: Int): Int = {
    requirements.edgeValue(start, u)
  }

  /**
   * Return the number of time points in the STN
 *
   * @return
   */
  def size: Int = requirements.numVertices - emptySpots.size

  def addRequirement(from: Int, to: Int, value: Int): Boolean = {
    val e = new E(from, to, new Requirement[ID](value))
    allConstraints = e :: allConstraints
    addEdge(e).nonEmpty
  }

  def addRequirementWithID(from:Int, to:Int, value:Int, id:ID) : Boolean = {
    val e = new E(from, to, new RequirementWithID[ID](value, id))
    allConstraints = e :: allConstraints
    addEdge(e).nonEmpty
  }

  def addContingent(from: Int, to: Int, lb: Int, ub:Int): Boolean = {
    val added = addContingent(from, to, ub, None) ++ addContingent(to, from, -lb, None)
    allConstraints = allConstraints ++added
    added.nonEmpty
  }

  def addContingentWithID(from:Int, to:Int, lb:Int, ub:Int, id:ID) : Boolean = {
    val added = addContingent(from, to, ub, Some(id)) ++ addContingent(to, from, -lb, Some(id))
    allConstraints = allConstraints ++ added
    added.nonEmpty
  }

  private def addContingent(from:Int, to:Int, value:Int, optID:Option[ID]) : List[E] = {
    val eCont = optID match {
      case Some(id) => new E(from, to, new ContingentWithID[ID](value, id))
      case None => new E(from, to, new Contingent[ID](value))
    }
    val eReq = optID match {
      case Some(id) => new E(from, to, new RequirementWithID[ID](value, id))
      case None => new E(from, to, new Requirement(value))
    }
    addEdge(eCont) ++ addEdge(eReq) //TODO
  }

  override protected[stnu] def addContingent(from: Int, to: Int, d: Int): Unit =
    allConstraints = allConstraints ++ addContingent(from, to, d, None)

  override protected[stnu] def addContingentWithID(from: Int, to: Int, d: Int, id: ID): Unit =
    allConstraints = allConstraints ++ addContingent(from, to, d, Some(id))

  protected def addConditional(from: Int, to: Int, on: Int, value: Int): Boolean = {
    while(!requirements.contains(from) || !requirements.contains(to) || !requirements.contains(on)) {
      addVar()
    }
    val e = new E(from, to, new Conditional(on, value))
    addEdge(e).nonEmpty
  }


  /**
   * Returns a complete clone of the STN.
 *
   * @return
   */
  def cc(): MMV[ID] = new MMV[ID](this)

  /** Returns true if the given requirement edge is present in the STNU */
  protected[stnu] def hasRequirement(from: Int, to: Int, value: Int): Boolean =
    requirements.edgeValue(from, to) == value

  def cycleDetected(): Unit = throw new RuntimeException("EDG should not be looking for cycles")
    //assert(!_consistent, "Should have been already detected through inconsistency.")

  /** Removes all constraints that were recorded with the given ID */
  override def removeConstraintsWithID(id: ID): Boolean = {
    allConstraints = allConstraints.filter((e:E) => e.l.optID match {
      case Some(constraintID) => id != constraintID
      case None => true
    })
    val prevSize = requirements.numVertices

    requirements = new FullIntIntDigraph(MMV.inf)
    contingents = new DirectedSimpleLabeledIIAdjList[Contingent[ID]]()
    conditionals = new DirectedMultiLabeledIIAdjList[Conditional[ID]]()

    _consistent = true
    modified = Nil

    for(i <- 0 until prevSize)
      addVar()
    for(e <- allConstraints)
      addEdge(e)

    checkConsistencyFromScratch()
  }

  /** Remove a variable and all constraints that were applied on it; */
  override def removeVar(u: Int): Boolean = ???

  /** Returns a collection of all time points in this STN */
  override def events: IList[Int] = (0 until requirements.numVertices).filter(!emptySpots.contains(_))

  override def controllability: Controllability = DYNAMIC_CONTROLLABILITY

  /** Adds a controllable variable. Only those variable can be executed */
  override def addDispatchable(): Int = {
    val i = addVar()
    dispatchableVars = dispatchableVars + i
    i
  }

  /** Adds a contingent variable */
  override def addContingentVar(): Int = {
    val i = addVar()
    contingentVars = contingentVars + i
    i
  }

  private def optID(e:E) : Option[ID] = e match {
    case e:LabeledEdgeWithID[_,_,ID] => Some(e.id)
    case _ => None
  }

  override def constraints: IList[(Int, Int, Int, ElemStatus, Option[ID])] = allConstraints.map((e:E) => {
    if(e.l.cont) (e.u, e.v, e.l.value, CONTINGENT, optID(e))
    else if(e.l.req) (e.u, e.v, e.l.value, CONTROLLABLE, optID(e))
    else throw new RuntimeException("This constraints should not be recorded: "+e)
  })

  override def isContingent(v: Int): Boolean = contingentVars.contains(v)

  /** Returns true if a variable is dispatchable */
  override def isDispatchable(v: Int): Boolean = dispatchableVars.contains(v)

  def executeVar(u:Int, atTime:Int): Unit = {
    assert(!isExecuted(u))
    assert(dispatchableVars.contains(u))
    assert(latestStart(u) >= atTime)
    assert(isEnabled(u))
    this.enforceInterval(start, u, atTime, atTime)
    setExecuted(u)
    enabled = enabled ++ events.filter(isEnabled(_))
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
    enabled = enabled ++ events.filter(isEnabled(_))
    checkConsistency()
  }

  protected def removeContingentOn(n:Int): Unit = {
    contingents.deleteEdges((e:E) => {
      e.v == n && e.l.positive || e.u == n && e.l.negative
    })
  }

  protected def removeWaitsOn(u:Int) = {
    conditionals.deleteEdges((e:E) => e.l.cond && e.l.node == u)
  }

  override def isEnabled(u:Int): Boolean = {
    if(isExecuted(u))
      return false

    for(e <- requirements.outEdges(u)) {
      if (e.l <= 0 && isContingent(e.v) && !executed.contains(e.v))
        return false
      if (e.l < 0 && dispatchableVars.contains(e.v) && !executed.contains(e.v))
        return false
    }

    for(e <- conditionals.outEdges(u)) {
      if(!isExecuted(e.l.node))
        if(earliestStart(e.v) >= MMV.inf || earliestStart(e.v) - e.l.value > earliestStart(u))
          return false
    }
    true
  }

  override def isExecuted(n: Int): Boolean = executed.contains(n)

  override def minDelay(from: Int, to: Int): Option[Int] = Some(-requirements.edgeValue(to, from))

  override def maxDelay(from: Int, to: Int): Option[Int] = Some(requirements.edgeValue(from, to))

  override def dispatchableEvents(atTime: Int): Iterable[Int] = {
    checkConsistency()
    enabled ++= events.filter(isEnabled(_))
    val executables = dispatchableVars.filter(n =>
      enabled.contains(n) && !executed.contains(n) && isLive(n, atTime))
    executables
  }

  override def isConstraintPossible(u: Int, v: Int, w: Int): Boolean =
    requirements.edgeValue(v, u) + w >= 0 // should not create a negative cycle

  override def setExecuted(n: Int): Unit = executed = executed + n


  def tightens(e : E) : Boolean = {
    e.l match {
      case req:Requirement[ID] => {
        req.value < requirements.edgeValue(e.u, e.v)
      }
      case cont:Contingent[ID] => {
        contingents.edge(e.u, e.v) match {
          case None => true
          case Some(prev) =>
            if(prev.l.value == e.l.value) false
            else throw new RuntimeException("Error: tightening/relaxing a contingent constraint.")
        }
      }
      case cond:Conditional[ID] => {
        conditionals.edges(e.u, e.v).forall(prev => prev.l.node != cond.node || cond.value < prev.l.value)
      }
    }
  }

  private def addEdge(e : E) : List[E] = {
    if(!tightens(e)) {
      // this edge is not tightening, nothing to add
      Nil
    } else {
      if(e.l.req) {
        requirements.addEdge(e.u, e.v, e.l.value)
        if(enabledIFPC)
          IFPC(e.u, e.v, e.l.value)
      } else if(e.l.cond) {
        conditionals.addEdge(e.asInstanceOf[LabeledEdge[Int, Conditional[ID]]])
      } else if(e.l.cont) {
        contingents.addEdge(e.asInstanceOf[LabeledEdge[Int, Contingent[ID]]])
        for(e <- requirements.inEdges(e.u) ++ requirements.outEdges(e.u) ++ requirements.inEdges(e.v) ++ requirements.outEdges(e.v)) {
          val y = new E(e.u, e.v, new Requirement[ID](e.l))
          modified = y::modified
        }

      } else {
        throw new RuntimeException("Error: Unknown constraint type.")
      }
      edgeAdded(e)

      // return the added edges
      List(e)
    }
  }

  private def edgeAdded(e : E) {
    assert(e.l.value < MMV.inf, "Infinite edge added : "+e)
    if(e.l.cont && dist(e.u, e.v) < e.l.value) {
      _consistent = false
    } else if(e.l.req) {
      contingents.edge(e.u, e.v) match {
        case Some(cont) if cont.l.value > e.l.value => {
          _consistent = false
        }
        case _ =>
      }
    }


    if(e.u == e.v && e.l.value < 0)
      _consistent = false

    val vuVal = requirements.edgeValue(e.v,e.u)
    if(plus(e.l.value, vuVal) < 0)
    // adding an edge with lower upper bound than lower bound
      _consistent = false

    modified = e :: modified
//    checkConsistency()
  }

  private final def plus(a:Int, b:Int) =
    if(a >= MMV.inf) Int.MaxValue
    else if(b >= MMV.inf) Int.MaxValue
    else a + b

    /** Computes the all pair shortest path inside the Requirements graph */
  protected[stnu] def apsp() = {
    for(k <- 0 until size)
      for(i <- 0 until size)
        for(j <- 0 until size)
          if(dist(i,j) > plus(dist(i,k),dist(k,j))) {
            addEdge(new E(i, j, new Requirement(plus(dist(i, k), dist(k, j)))))
          }
  }

  protected[stnu] def IFPC(a:Int, b:Int, d:Int): Unit = {
    var I = List[Int]()
    var J = List[Int]()

    for(k <- events ; if k != a && k != b) {
      val kab = plus(dist(k, a), dist(a, b))
      if (dist(k, b) > kab) {
        requirements.addEdge(k, b, kab)
        edgeAdded(new E(k, b, new Requirement[ID](kab)))
        I = k :: I
      }
      val abk = plus(dist(a, b), dist(b, k))
      if (dist(a, k) > abk) {
        requirements.addEdge(a, k, abk)
        edgeAdded(new E(a, k, new Requirement[ID](abk)))
        J = k :: J
      }
    }
    for(i <- I ; j <- J ; if i != j) {
      val iaj = plus(dist(i, a), dist(a, j))
      if(dist(i, j) > iaj) {
        requirements.addEdge(i, j, iaj)
        edgeAdded(new E(i, j, new Requirement[ID](iaj)))
      }
    }
  }

  /** Returns Some((min, max)) if there is a contingent constraint from --[min,max]--> to.
    * Returns None otherwise.
    */
  override def getContingentDelay(from: Int, to: Int): Option[(Int, Int)] =
    (contingents.edge(to, from), contingents.edge(from, to)) match {
      case (Some(min), Some(max)) => Some((-min.l.value, max.l.value))
      case (None, None) => None
      case _ => throw new RuntimeException("This contingent constraint does not seem symmetrical.")
    }

  /** All derivations made with FastIDC's derivations with e as a focus edges.
    *
    * Returns a tuple (edges to add, edges to remove).
    */
  def classicalDerivations(e : E) =
    if(e.l.req && e.l.value >= MMV.inf)
      Nil
    else
      PR1(e) :: PR2(e) :: unorderedRed(e) :: SR1(e) :: contingentReg(e) :: unconditionalRed(e) :: generalRed(e) :: Nil

  protected[stnu] def PR1(e : E) : (List[E], List[E]) = D6(e)

  protected[stnu] def PR2(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    val reqBA = requirements.edgeValue(B, A)
    // AB must be positive and BA negative (A before B)
    if(e.l.posReq && reqBA <= 0) {
      for (contCB <- contingents.inEdges(B) ; if contCB.l.positive) {
        val C = contCB.u
        if (A != B && A != C && B != C) {
          toAdd = new E(A, C, new Requirement(e.l.value - contCB.l.value)) :: toAdd
        }
      }
    }
    (toAdd, Nil)
  }

  protected[stnu] def unorderedRed(e : E) : (List[E], List[E]) = D1(e)

  protected[stnu] def SR1(e : E) : (List[E], List[E]) = {
    val (add3, rm3) = D3(e)
    val (add5, rm5) = D5(e)
    (add3 ++ add5, rm3 ++ rm5)
  }

  protected[stnu] def SR2(e : E) : (List[E], List[E]) = throw new RuntimeException("This rule is useless")

  protected[stnu] def contingentReg(e : E) : (List[E], List[E]) = D2(e)

  protected[stnu] def unconditionalRed(e : E) : (List[E], List[E]) = D8(e)

  protected[stnu] def generalRed(e : E) : (List[E], List[E]) = D9(e)

  protected[stnu] def D1(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    if(e.l.posReq) {
      for(C <- contingents.children(B) ; if A != B && A != C && B != C) {
        (contingents.edge(B, C), contingents.edge(C, B)) match {
          case (Some(bc), Some(cb)) => {
            if(bc.l.negative && cb.l.positive) {
              toAdd = new E(A, C, new Conditional(B, e.l.value - cb.l.value)) :: toAdd
            }
          }
          case _ =>
        }
      }
    }
    (toAdd, Nil)
  }

  protected[stnu] def D2(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond && e.l.negative) {
      val B = e.l.node
      val outEdges = contingents.outEdges(C)
      for(D <- contingents.children(C) ; if D != C ; if A != B && A != C && B != C) {
        if(contingents.edges(C, D).nonEmpty && contingents.edges(D, C).nonEmpty) {
          val cd = contingents.edges(C, D).head
          val dc = contingents.edges(D, C).head
          if(dc.l.positive && cd.l.negative) {
            toAdd = new E(D, A, new Conditional(B, -cd.l.value + e.l.value)) :: toAdd
          }
        }
      }
    }

    (toAdd, Nil)
  }

  protected[stnu] def D3(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond && e.l.negative) {
      val B = e.l.node
      for(D <- requirements.parents(C) ; if D != C ; if A != B && A != C && B != C) {
        val dc = requirements.edges(D, C).head
        if(D != B && dc.l >= 0 && dc.l < MMV.inf) {
          toAdd = new E(D, A, new Conditional(B, dc.l + e.l.value)) :: toAdd
        }
      }
    }

    (toAdd, Nil)
  }

  protected[stnu] def D5(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    if(e.l.req && e.l.positive) {
      for(condBC <- conditionals.outEdges(B)) {
        val C = condBC.v
        val D = condBC.l.node
        if(A != B && A != C && B != C &&condBC.l.negative && D != A) {
          toAdd = new E(A, C, new Conditional(D, e.l.value+condBC.l.value)) :: toAdd
        }
      }
    }

    (toAdd, Nil)
  }

  protected[stnu] def D6(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val B = e.u
    if(e.l.req && e.l.negative) {
      for(contBC <- contingents.outEdges(B) ; if contBC.l.negative) {
        val C = contBC.v
        if (A != B && A != C && B != C) {
          contingents.edge(C, B) match {
            case None =>
            case Some(contCB) =>
              toAdd = new E(C, A, new Requirement(-contBC.l.value + e.l.value)) :: toAdd
          }
        }
      }
    }

    (toAdd, Nil)
  }

  protected[stnu] def D8(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    var toRemove = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond && e.l.negative) {
      val B = e.l.node
      for(reqBA <- contingents.edges(B, A) ;
          if reqBA.l.negative ;
          if e.l.value >= reqBA.l.value ;
          if A != B && A != C && B != C) {
        toAdd = new E(C, A, new Requirement(e.l.value)) :: toAdd
        if(!toRemove.contains(e))
          toRemove = e :: toRemove
      }
    }
    (toAdd, toRemove)
  }

  protected[stnu] def D9(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond) {
      val B = e.l.node
      for(reqBA <- contingents.edges(B, A) ;
          if e.l.value < reqBA.l.value ;
          if A != B && A != C && B != C) {
        toAdd = new E(C, A, new Requirement(reqBA.l.value)) :: toAdd
      }
    }
    (toAdd, Nil)
  }
}

object MMV {
  final val inf = 2000000000
}