package fr.laas.fape.constraints.stn

import fr.laas.fape.anml.model.concrete.TPRef
import fr.laas.fape.constraints.stn.bellmanford.CoreSTNIncBellmanFord
import fr.laas.fape.constraints.stnu.nilsson.FastIDC
import fr.laas.fape.constraints.stnu.pseudo.{MinimalSTNUManager, PseudoSTNUManager}
import fr.laas.fape.constraints.stnu._

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
