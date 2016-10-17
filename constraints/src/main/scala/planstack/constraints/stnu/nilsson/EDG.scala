package planstack.constraints.stnu.nilsson

import planstack.constraints.stn.Weight
import planstack.constraints.stnu.Edge
import planstack.graph.GraphFactory
import planstack.graph.core.impl.intindexed.{DirectedMultiLabeledIIAdjList, DirectedSimpleLabeledIIAdjList}
import planstack.graph.core.impl.matrix.{FullIntIntDigraph, SimpleLabeledDirectedIIMatrix}
import planstack.graph.core.{LabeledEdge, SimpleLabeledDigraph}
import planstack.graph.printers.NodeEdgePrinter

/** Objects implementing this interface can be passed to an EDG instance
  * to react to events occurring in an EDG such as edge addition, negative cycle ...
  */
trait EDGListener[ID] {
  def edgeAdded(e : LabeledEdge[Int, STNULabel[ID]])
  def inconsistencyDetected()
  def cycleDetected()
  def squeezingDetected()
}

class EDG[ID](val checkCycles : Boolean,
              val contingents : DirectedSimpleLabeledIIAdjList[Contingent[ID]],
              val requirements : SimpleLabeledDirectedIIMatrix[Requirement[ID]],
              val conditionals : DirectedMultiLabeledIIAdjList[Conditional[ID]],
              val ccgraph : CCGraph,
              var squeezed : Boolean,
              protected[stnu] var listener : EDGListener[ID])
{
  type E = Edge[ID]

  def this(edg : EDG[ID], listener:EDGListener[ID] = null) =
    this(edg.checkCycles,
      edg.contingents.cc(),
      edg.requirements.cc(),
      edg.conditionals.cc(),
      if(edg.checkCycles) edg.ccgraph.cc() else null,
      edg.squeezed,
      listener)

  def this(checkCycles:Boolean) = this(
    checkCycles,
    new DirectedSimpleLabeledIIAdjList[Contingent[ID]](),
    new SimpleLabeledDirectedIIMatrix[Requirement[ID]](),
    new DirectedMultiLabeledIIAdjList[Conditional[ID]](),
    if(checkCycles) new CCGraph else null,
    false,
    null
  )

  def inConditionals(n : Int) = conditionals.inEdges(n).filter(_.l.cond)
  def outConditionals(n : Int) = conditionals.outEdges(n).filter(_.l.cond)
  def inRequirements(n : Int) = requirements.inEdges(n).filter(_.l.req)
  def inPosReq(n : Int) = requirements.inEdges(n).filter(_.l.posReq)
  def inNegReq(n : Int) = requirements.inEdges(n).filter(_.l.negReq)
  def outRequirements(n : Int) = requirements.outEdges(n).filter(_.l.req)
  def outPosReq(n : Int) = requirements.outEdges(n).filter(_.l.posReq)
  def outNegReq(n : Int) = requirements.outEdges(n).filter(_.l.negReq)
  def inContingent(n : Int) = contingents.inEdges(n).filter(_.l.cont)
  def outContingent(n : Int) = contingents.outEdges(n).filter(_.l.cont)

  def size = requirements.numVertices

  /**
   * Creates a new time point and returns its ID. New constraints are inserted to place it before end and after start.
   *
   * @return ID of the created time point
   */
  def addVar(): Int = {
    requirements.addVertex()
    conditionals.addVertex()
    contingents.addVertex()
  }

  def allEdges = requirements.edges() ++ contingents.edges() ++ conditionals.edges()

  def edgesOn(tp:Int) =
    requirements.inEdges(tp) ++ requirements.outEdges(tp) ++ contingents.inEdges(tp) ++
      contingents.outEdges(tp) ++ conditionals.inEdges(tp) ++ conditionals.outEdges(tp)

  def tightens(e : E) : Boolean = {
    e.l match {
      case req:Requirement[ID] => {
        requirements.edge(e.u, e.v) match {
          case None => true
          case Some(prev) => req.value < prev.l.value
        }
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

  def hasNegativeCycles = !ccgraph.acyclic

  protected[stnu] def addEdge(e : E) : List[E] = {
    if(!tightens(e)) {
      // this edge is not tightening, nothing to add
      Nil
    } else {
      if(e.l.req) {
        requirements.addEdge(e.asInstanceOf[LabeledEdge[Int, Requirement[ID]]])
      } else if(e.l.cond)
        conditionals.addEdge(e.asInstanceOf[LabeledEdge[Int, Conditional[ID]]])
      else if(e.l.cont)
        contingents.addEdge(e.asInstanceOf[LabeledEdge[Int, Contingent[ID]]])
      else
        throw new RuntimeException("Error: Unknown constraint type.")


      edgeAdded(e)

      // return the added edges
      List(e)
    }
  }

  def edgeAdded(e : E) {
    if(checkCycles && e.l.negative)
      ccgraph.addEdge(e.u, e.v)

    if(e.l.cont) {
      requirements.edge(e.u, e.v) match {
        case Some(req) => squeezed |= e.l.value > req.l.value
        case None =>
      }
    } else if(e.l.req) {
      contingents.edge(e.u, e.v) match {
        case Some(cont) => squeezed |= cont.l.value > e.l.value
        case None =>
      }

    }

    if(listener != null) {
      if(checkCycles && !ccgraph.acyclic)
        listener.cycleDetected()

      if(squeezed)
        listener.squeezingDetected()

      if(e.u == e.v && e.l.value < 0)
        listener.inconsistencyDetected()

      val vuVal = requirements.edgeValue(e.v,e.u)
      if(vuVal != null && e.l.value < -vuVal.value)
      // adding an edge with lower upper bound than lower bound
        listener.inconsistencyDetected()

      listener.edgeAdded(e)
    }
  }


  def addRequirement(from:Int, to:Int, value:Int, optID:Option[ID] = None) : List[E] = {
    while(!requirements.contains(from) || !requirements.contains(to)) {
      addVar()
    }

    val e = optID match {
      case Some(id) => new E(from, to, new RequirementWithID[ID](value, id))
      case None => new E(from, to, new Requirement(value))
    }
    addEdge(e)
  }

  def addContingent(from:Int, to:Int, value:Int, optID:Option[ID] = None) : List[E] = {
    while(!requirements.contains(from) || !requirements.contains(to)) {
      addVar()
    }
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

  def addConditional(from:Int, to:Int, on:Int, value:Int) : List[E] = {
    while(!requirements.contains(from) || !requirements.contains(to) || !requirements.contains(on)) {
      addVar()
    }
    val e = new E(from, to, new Conditional(on, value))
    addEdge(e)
  }

  /** Computes the all pair shortest path inside the Requirements graph */
  protected[stnu] def apsp() = {
    def dist(i:Int, j:Int) =
      if(i == j)  0
      else requirements.edge(i, j) match {
        case Some(req) => req.l.value
        case None => Int.MaxValue
      }
    def plus(a:Int, b:Int) =
      if(a == Int.MaxValue) a
      else if(b == Int.MaxValue) b
      else a + b

    for(k <- 0 until size)
      for(i <- 0 until size)
        for(j <- 0 until size)
          if(dist(i,j) > plus(dist(i,k),dist(k,j))) {
            addEdge(new E(i, j, new Requirement(plus(dist(i, k), dist(k, j)))))
          }
  }

  /** All derivations made with FastIDC's derivations with e as a focus edges.
    *
    * Returns a tuple (edges to add, edges to remove).
    */
  def derivationsFastIDC(e : E) : List[(List[E], List[E])] =
    D1(e) :: D2(e) :: D3(e) :: D4(e) :: D5(e) :: D6(e) :: D7(e) :: D8(e) :: D9(e) :: Nil

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
        if(D != B && dc.l.positive) {
          toAdd = new E(D, A, new Conditional(B, dc.l.value+e.l.value)) :: toAdd
        }
      }
    }

    (toAdd, Nil)
  }

  protected[stnu] def D4(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    if(e.l.req && e.l.positive) {
      for(C <- 0 until requirements.numVertices ; if A != B && A != C && B != C) {
        val BCvalue = requirements.edgeValue(B, C)
        if(BCvalue != null && BCvalue.negative) {
          toAdd = new E(A, C, new Requirement(e.l.value + BCvalue.value)) :: toAdd
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

  protected[stnu] def D7(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val B = e.u
    if(e.l.req && e.l.negative) {
      for(reqCB <- requirements.inEdges(B) ; if reqCB.l.positive) {
        val C = reqCB.u
        if(A != B && A != C && B != C)
          toAdd = new E(C, A, new Requirement(reqCB.l.value + e.l.value)) :: toAdd
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

  /** All derivations made with FastIDC's derivations with e as a focus edges.
    *
    * Returns a tuple (edges to add, edges to remove).
    */
  def classicalDerivations(e : E) =
    PR1(e) :: PR2(e) :: unorderedRed(e) :: SR1(e) :: contingentReg(e) :: unconditionalRed(e) :: generalRed(e) :: Nil

  protected[stnu] def PR1(e : E) : (List[E], List[E]) = D6(e)

  protected[stnu] def PR2(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    val reqBA = requirements.edgeValue(B, A)
    // AB must be positive and BA negative (A before B)
    if(e.l.posReq && reqBA != null && reqBA.value <= 0) {
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

  def exportToDot(file:String, printer:NodeEdgePrinter[Int, STNULabel[ID], E]) {
    val g = GraphFactory.getLabeledDigraph[Int, STNULabel[ID]]
    for(i <- 0 until requirements.numVertices) {
      g.addVertex(i)
    }
    for(e <- requirements.edges()) g.addEdge(e)
    for(e <- contingents.edges()) g.addEdge(e)
    for(e <- conditionals.edges()) g.addEdge(e)

    g.exportToDotFile(file, printer)
  }

  protected[stnu] def hasRequirement(from:Int, to:Int, value:Int) = requirements.edge(from, to) match {
    case None => false
    case Some(req) => req.l.value == value
  }

  /** Bellman ford algorithm, returns the minimal distance between from and to.
    *  Each call is O(VE) */
  protected[stnu] def minDist(from:Int, to:Int): Int = {
    val forwardDist = scala.collection.mutable.Map[Int,Weight]()
    // set all distances to inf except for the origin
    for(v <- 0 until size) {
      val initialDist =
        if(v == from) new Weight(0)
        else Weight.InfWeight
      forwardDist(v) = initialDist
    }

    // O(n*e): recomputes check all edges n times. (necessary because the graph is cyclic with negative values
    var i = 0
    var updated = true
    while(i < size && updated) {
      updated = false
      i += 1
      for(e <- requirements.edges()) {
        if(forwardDist(e.u) + e.l.value < forwardDist(e.v)) {
          forwardDist(e.v) = forwardDist(e.u) + e.l.value
          updated = true
        }
      }
    }

    if(forwardDist(to).inf)
      Int.MaxValue
    else
      forwardDist(to).w
  }

  protected[stnu] def apspGraph() : SimpleLabeledDigraph[Int,Int] = {
    val matrix = new FullIntIntDigraph(size, Int.MaxValue)
    for(i <- 0 until size)
      matrix.addVertex()
    for(e <- requirements.edges())
      matrix.addEdge(e.u, e.v, e.l.value)

    def dist(i:Int, j:Int) =
      if(i == j)  0
      else matrix.edgeValue(i, j)

    def plus(a:Int, b:Int) =
      if(a == Int.MaxValue) a
      else if(b == Int.MaxValue) b
      else a + b

    for(k <- 0 until size)
      for(i <- 0 until size)
        for(j <- 0 until size)
          if(dist(i,j) > plus(dist(i,k),dist(k,j))) {
            matrix.addEdge(i, j, plus(dist(i, k), dist(k, j)))
            if(plus(dist(i,j), dist(j,i))<0)
              listener.cycleDetected()
          }

    matrix
  }
}
