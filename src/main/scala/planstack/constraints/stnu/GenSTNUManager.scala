package planstack.constraints.stnu

import planstack.constraints.stn.GenSTNManager
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter



trait GenSTNUManager[TPRef,ID] extends GenSTNManager[TPRef,ID] {

  def enforceContingent(u:TPRef, v:TPRef, min:Int, max:Int)

  def enforceContingentWithID(u:TPRef, v:TPRef, min:Int, max:Int, id:ID)

  def checksPseudoConsistency : Boolean
}


