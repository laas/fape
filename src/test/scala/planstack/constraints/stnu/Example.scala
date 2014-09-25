package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter

class Example extends FunSuite {

  val idc = new FastIDC()

  val A = idc.addVar()
  val B = idc.addVar()
  val C = idc.addVar()

  val printer = new NodeEdgePrinter[Int, STNULabel, LabeledEdge[Int,STNULabel]] {
    override def printNode(n:Int) = n match {
      case A => "A"
      case B => "B"
      case C => "C"
      case 0 => "start"
      case 1 => "end"
      case _ => n.toString
    }
    override def printEdge(el : STNULabel) = {
      if(el.cond) "<%s, %d>".format(printNode(el.node), el.value)
      else el.toString
    }
    //override def excludeNode(n:Int) = n == 1 || n == 0
  }

  idc.addRequirement(A, B, 10)
  idc.consistent

  idc.addContingent(C, B, 35, 40)

  idc.consistent

//  idc.edg.exportToDot("incremental.dot", printer)
  val full = idc.cc()
  full.checkConsistencyFromScratch()
  full.checkConsistencyFromScratch()
  full.checkConsistencyFromScratch()
  full.checkConsistencyFromScratch()
//  full.edg.exportToDot("full.dot", printer)

}
