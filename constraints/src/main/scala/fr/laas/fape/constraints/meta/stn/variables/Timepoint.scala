package fr.laas.fape.constraints.meta.stn.variables

import fr.laas.fape.constraints.meta.variables.IVar

class Timepoint(id: Int, ref: Option[Any]) extends IVar(id) {

  def isStructural : Boolean = false
  def isContingent : Boolean = false

}
