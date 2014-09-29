package planstack.constraints.stn

import planstack.constraints.stnu._

object Predef {

  def getAllISTN[ID] : List[ISTN[ID]] = List(
    new STNIncBellmanFord[ID](),
    new FastIDC[ID](),
    new FullSTN[ID](10)
  )

  def getAllISTNU[ID] : List[ISTNU[ID]] = List(
    new FastIDC[ID]()
  //new EfficientIDC[ID]
  )

  def getAllSTNUManager[TPRef,ID] : List[GenSTNUManager[TPRef,ID]] = List(
    new PseudoSTNUManager[TPRef,ID]()
  )

  def getAllSTNManager[TPRef,ID] : List[GenSTNManager[TPRef,ID]] = List(
    new STNManager[TPRef,ID](),
    new PseudoSTNUManager[TPRef,ID]()
  )

}
