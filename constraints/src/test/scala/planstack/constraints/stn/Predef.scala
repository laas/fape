package planstack.constraints.stn

import planstack.anml.model.concrete.TPRef
import planstack.constraints.stn.bellmanford.CoreSTNIncBellmanFord
import planstack.constraints.stnu._
import planstack.constraints.stnu.nilsson.FastIDC
import planstack.constraints.stnu.pseudo.{MinimalSTNUManager, PseudoSTNUManager}

object Predef {

  def getAllISTN[ID] : List[CoreSTN[ID]] = List(
    new CoreSTNIncBellmanFord[ID](),
    new FastIDC[ID](),
    new FullSTN[ID](10),
    new MMV[ID]()
  )

  def getAllISTNU[ID] : List[CoreSTNU[ID]] = List(
    new FastIDC[ID](),
    new MMV[ID]()
  //new EfficientIDC[ID]
  )

  def getAllSTNUManager[ID] : List[GenSTNUManager[ID]] = List(
    new MinimalSTNUManager[ID](),
    new PseudoSTNUManager[ID](),
    new STNUManager[ID]()
  )

  def getAllSTNManager[ID] : List[STN[TPRef,ID]] = List(
    new MinimalSTNUManager[ID](),
    new PseudoSTNUManager[ID](),
    new STNUManager[ID]()
  )

}
