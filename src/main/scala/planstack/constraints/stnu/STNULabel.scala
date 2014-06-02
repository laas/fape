package planstack.constraints.stnu

import planstack.graph.GraphFactory
import planstack.graph.core.impl.intindexed.{DirectedSimpleLabeledIIAdjList, DirectedMultiLabeledIIAdjList}
import planstack.graph.core.LabeledEdge
import scala.collection.mutable.ListBuffer
import com.sun.xml.internal.bind.v2.runtime.RuntimeUtil.ToStringAdapter
import planstack.constraints.stn.STN

class STNULabel(val value : Int) {

  def req = this.isInstanceOf[Requirement]
  def posReq = this.isInstanceOf[Requirement] && value >= 0
  def negReq = this.isInstanceOf[Requirement] && value < 0
  def cont = this.isInstanceOf[Contingent]
  def cond = this.isInstanceOf[Conditional]
  def negative = value < 0
  def positive = value >= 0
  def node : Int = throw new RuntimeException("This label has no conditional node: "+this)
}

class Requirement(value : Int) extends STNULabel(value)

class Contingent(value : Int) extends STNULabel(value)

class Conditional(override val node : Int, value : Int) extends  STNULabel(value)



class STNU {
  type E = LabeledEdge[Int, STNULabel]


  val contingents = new DirectedSimpleLabeledIIAdjList[Contingent]()
  val requirements = new DirectedSimpleLabeledIIAdjList[Requirement]()
  val conditionals = new DirectedMultiLabeledIIAdjList[Conditional]()

  val ddg = new DirectedMultiLabeledIIAdjList[Int]()

  /** Contains all negative edges */
  val ccgraph = GraphFactory.getSimpleUnlabeledDigraph[Int]

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


  def addRequirement(from:Int, to:Int, value:Int) {
    while(!requirements.contains(from) || !requirements.contains(to)) {
      requirements.addVertex()
      conditionals.addVertex()
      contingents.addVertex()
    }

    val e = new LabeledEdge[Int, Requirement](from, to, new Requirement(value))
    requirements.addEdge(e)

//    efficientIDC(e)
  }

  def addContingent(from:Int, to:Int, value:Int) {
    while(!requirements.contains(from) || !requirements.contains(to)) {
      requirements.addVertex()
      conditionals.addVertex()
      contingents.addVertex()
    }
    val e = new LabeledEdge[Int, Contingent](from, to, new Contingent(value))
    contingents.addEdge(e)

//    efficientIDC(e)
  }


  def addConditional(from:Int, to:Int, on:Int, value:Int) {
    while(!requirements.contains(from) || !requirements.contains(to)) {
      requirements.addVertex()
      conditionals.addVertex()
      contingents.addVertex()
    }
    val e = new LabeledEdge[Int, Conditional](from, to, new Conditional(on, value))
    conditionals.addEdge(e)
  }

  def checkCycleOfNegative() : Boolean = false

  /*
  def efficientIDC(e : LabeledEdge[Int, STNULabel]) : Boolean = {
    val todo = ListBuffer[Int]()

    todo += e.v

    e.l match  {
      case l:Contingent if l.value <0 && ccgraph.edge(e.u, e.v).isEmpty => {
        ccgraph.addEdge(e.u, e.v)
        if(!checkCycleOfNegative) return false
        if(!todo.contains(e.u)) todo += e.u
      }
      case _ => // nothing to do
    }

    while(todo.nonEmpty) {
      val current = todo.filter(n => {
        ccgraph.inEdges(n).forall(e => !todo.contains(e.u))
      }).headOption match {
        case Some(node) => node
        case None => throw new RuntimeException("Error: unable to select a current node")
      }

      processCond(current)
      processNegReq(current)
      processPosReq(current)
    }

    true
  }
  */

  def processCond(n : Int) {

  }

  def processNegReq(n : Int) {

  }

  def processPosReq(n : Int) {

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
              toAdd = (new E(A, C, new Conditional(B, e.l.value - cb.l.value))) :: toAdd
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
      for(req <- requirements.outEdges(B)) {
        val C = req.v
        if(req.l.negative) {
          toAdd = new E(A, C, new Requirement(e.l.value + req.l.value)) :: toAdd
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
}