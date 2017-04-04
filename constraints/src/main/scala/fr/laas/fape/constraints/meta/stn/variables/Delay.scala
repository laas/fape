package fr.laas.fape.constraints.meta.stn.variables

import fr.laas.fape.constraints.meta.variables.IVar

class Delay(val from: Timepoint, val to: Timepoint, id: Int) extends IVar(id) {

}
