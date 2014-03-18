package planstack.anml.model.concrete

import planstack.anml.model.TPRef

object Timepoint {

  private var nextID = 0

  type Timepoint = TPRef

  def apply() : Timepoint = {
    nextID += 1
    new TPRef("tp_"+(nextID-1))
  }

}
