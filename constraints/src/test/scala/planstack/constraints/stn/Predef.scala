package planstack.constraints.stn

import planstack.anml.model.concrete.TPRef
import planstack.constraints.stnu._

object Predef {

  def getAllISTN[ID] : List[ISTN[ID]] = List(
    new STNIncBellmanFord[ID](),
    new FastIDC[ID](),
    new FullSTN[ID](10),
    new MMV[ID]()
  )

  def getAllISTNU[ID] : List[ISTNU[ID]] = List(
    new FastIDC[ID](),
    new MMV[ID]()
  //new EfficientIDC[ID]
  )

  def getAllSTNUManager[ID] : List[GenSTNUManager[ID]] = List(
    new MinimalSTNUManager[ID](),
    new PseudoSTNUManager[ID](),
    new STNUManager[ID]()
  )

  def getAllSTNManager[ID] : List[GenSTNManager[TPRef,ID]] = List(
    new MinimalSTNUManager[ID](),
    new PseudoSTNUManager[ID](),
    new STNUManager[ID]()
  )

}
