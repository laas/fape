package fr.laas.fape.constraints.meta.stn.core

import fr.laas.fape.constraints.meta.stn.variables.Timepoint

trait IDistanceChangeListener {

  def distanceUpdated(tp1: Timepoint, tp2: Timepoint)

}
