package planstack.constraints.stnu

import planstack.graph.GraphFactory
import planstack.graph.core.LabeledEdge
import planstack.graph.core.impl.intindexed.{DirectedMultiLabeledIIAdjList, DirectedSimpleLabeledIIAdjList}
import planstack.graph.core.impl.matrix.SimpleLabeledDirectedIIMatrix
import planstack.graph.printers.NodeEdgePrinter

/** Objects implementing this interface can be passed to an EDG instance
  * to react to events occurring in an EDG such as edge addition, negative cycle ...
  */
trait EDGListener {
  def edgeAdded(e : LabeledEdge[Int, STNULabel])
  def cycleDetected()
  def squeezingDetected()
}

class EDG(
    val contingents : DirectedSimpleLabeledIIAdjList[Contingent],
    val requirements : SimpleLabeledDirectedIIMatrix[Requirement],
    val conditionals : DirectedMultiLabeledIIAdjList[Conditional],
    val ccgraph : CCGraph,
    var squeezed : Boolean,
    protected[stnu] var listener : EDGListener) {

  def this(edg : EDG, listener:EDGListener = null) = this(edg.contingents.cc(), edg.requirements.cc(), edg.conditionals.cc(), edg.ccgraph.cc(), edg.squeezed, listener)

  def this(listener:EDGListener = null) = this(
    new DirectedSimpleLabeledIIAdjList[Contingent](),
    new SimpleLabeledDirectedIIMatrix[Requirement](),
    new DirectedMultiLabeledIIAdjList[Conditional](),
    new CCGraph,
    false,
    listener
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

  /*
  def squeezed = !contingents.edges().forall(cont => {
    requirements.edge(cont.u, cont.v) match {
      case Some(req) => {
        req.l.value >= cont.l.value
      }
      case None => true
    }
  })*/

  def tightens(e : E) : Boolean = {
    e.l match {
      case req:Requirement => {
        requirements.edge(e.u, e.v) match {
          case None => true
          case Some(prev) => req.value < prev.l.value
        }
      }
      case cont:Contingent => {
        contingents.edge(e.u, e.v) match {
          case None => true
          case Some(prev) => if(prev.l.value == e.l.value) false
            else throw new RuntimeException("Error: tightening/relaxing a contingent constraint.")
        }
      }
      case cond:Conditional => {
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
      if(e.l.req)
        requirements.addEdge(e.asInstanceOf[LabeledEdge[Int, Requirement]])
      else if(e.l.cond)
        conditionals.addEdge(e.asInstanceOf[LabeledEdge[Int, Conditional]])
      else if(e.l.cont)
        contingents.addEdge(e.asInstanceOf[LabeledEdge[Int, Contingent]])
      else
        throw new RuntimeException("Error: Unknown contraint type.")


      edgeAdded(e)

      // return the added edges
      List(e)
    }
  }

  def edgeAdded(e : E) {
    if(e.l.negative)
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
      if(!ccgraph.acyclic)
        listener.cycleDetected()

      if(squeezed)
        listener.squeezingDetected()

      listener.edgeAdded(e)
    }
  }


  def addRequirement(from:Int, to:Int, value:Int) : List[E] = {
    while(!requirements.contains(from) || !requirements.contains(to)) {
      addVar()
    }

    val e = new LabeledEdge[Int, Requirement](from, to, new Requirement(value))
    addEdge(e)
  }

  def addContingent(from:Int, to:Int, value:Int) : List[E] = {
    while(!requirements.contains(from) || !requirements.contains(to)) {
      addVar()
    }
    val e = new LabeledEdge[Int, Contingent](from, to, new Contingent(value))
    addEdge(e) ++ addEdge(new E(from, to, new Requirement(value))) //TODO
  }

  def addConditional(from:Int, to:Int, on:Int, value:Int) : List[E] = {
    while(!requirements.contains(from) || !requirements.contains(to) || !requirements.contains(on)) {
      addVar()
    }
    val e = new LabeledEdge[Int, Conditional](from, to, new Conditional(on, value))
    addEdge(e)
  }

  def D1(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    if(e.l.posReq) {
      for(C <- contingents.children(B)) {
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

  def D2(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond && e.l.negative) {
      val B = e.l.asInstanceOf[Conditional].node
      val outEdges = contingents.outEdges(C)
      for(D <- contingents.children(C)) {
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

  def D3(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond && e.l.negative) {
      val B = e.l.asInstanceOf[Conditional].node
      for(D <- requirements.parents(C)) {
        val dc = requirements.edges(D, C).head
        if(D != B && dc.l.positive) {
          toAdd = new E(D, A, new Conditional(B, dc.l.value+e.l.value)) :: toAdd
        }
      }
    }

    (toAdd, Nil)
  }

  def D4(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    if(e.l.req && e.l.positive) {
      for(C <- 0 until requirements.numVertices) {
        val BCvalue = requirements.edgeValue(B, C)
        if(BCvalue != null && BCvalue.negative) {
          toAdd = new E(A, C, new Requirement(e.l.value + BCvalue.value)) :: toAdd
        }
      }
    }

    (toAdd, Nil)
  }

  def D5(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.u
    val B = e.v
    if(e.l.req && e.l.positive) {
      for(condBC <- conditionals.outEdges(B)) {
        val C = condBC.v
        val D = condBC.l.node
        if(condBC.l.negative && D != A) {
          toAdd = new E(A, C, new Conditional(D, e.l.value+condBC.l.value)) :: toAdd
        }
      }
    }

    (toAdd, Nil)
  }

  def D6(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val B = e.u
    if(e.l.req && e.l.negative) {
      for(contBC <- contingents.outEdges(B) ; if contBC.l.negative) {
        val C = contBC.v
        contingents.edge(C, B) match {
          case None =>
          case Some(contCB) =>
            toAdd = new E(C, A, new Requirement(-contBC.l.value + e.l.value)) :: toAdd
        }
      }
    }

    (toAdd, Nil)
  }

  def D7(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val B = e.u
    if(e.l.req && e.l.negative) {
      for(reqCB <- requirements.inEdges(B) ; if reqCB.l.positive) {
        val C = reqCB.u
        toAdd = new E(C, A, new Requirement(reqCB.l.value + e.l.value)) :: toAdd
      }
    }

    (toAdd, Nil)
  }

  def D8(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    var toRemove = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond) {
      val B = e.l.node
      for(reqBA <- contingents.edges(B, A) ; if e.l.value >= reqBA.l.value) {
        toAdd = new E(C, A, new Requirement(e.l.value)) :: toAdd
        if(!toRemove.contains(e))
          toRemove = e :: toRemove
      }
    }
    (toAdd, toRemove)
  }

  def D9(e : E) : (List[E], List[E]) = {
    var toAdd = List[E]()
    val A = e.v
    val C = e.u
    if(e.l.cond) {
      val B = e.l.node
      for(reqBA <- contingents.edges(B, A) ; if e.l.value < reqBA.l.value) {
        toAdd = new E(C, A, new Requirement(reqBA.l.value)) :: toAdd
      }
    }
    (toAdd, Nil)
  }

  def exportToDot(file:String, printer:NodeEdgePrinter[Int, STNULabel, LabeledEdge[Int,STNULabel]]) {
    val g = GraphFactory.getLabeledDigraph[Int, STNULabel]
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
}
