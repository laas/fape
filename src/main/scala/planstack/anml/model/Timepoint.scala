package planstack.anml.model

object Timepoint {

  private var nextID = 0

  type Timepoint = Int

  def apply() : Timepoint = {
    nextID += 1
    nextID-1
  }

}
