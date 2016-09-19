package planstack.anml.model.concrete

import planstack.anml.model.{AnmlProblem, ChronicleContainer, Context}
import planstack.anml.model.abs.AbstractChronicle
import planstack.anml.model.abs.time.AbsTP
import planstack.anml.model.concrete.time.TimepointRef

trait ChronicleAnnotation

class ObservationConditionsAnnotation(val tp: TPRef, val conditions: Chronicle) extends ChronicleAnnotation with ChronicleContainer {
  conditions.container = Some(this)
  def label = "observation-conditions("+tp+")"
}




trait AbstractChronicleAnnotation {
  def getInstance(context: Context, temporalContext: TemporalInterval, pb: AnmlProblem, refCounter: RefCounter) : ChronicleAnnotation
}

class AbstractObservationConditionsAnnotation(val tp: AbsTP, val conditions: AbstractChronicle) extends AbstractChronicleAnnotation {
  override def getInstance(context: Context, temporalContext: TemporalInterval, pb: AnmlProblem, refCounter: RefCounter): ObservationConditionsAnnotation = {
    new ObservationConditionsAnnotation(
      TimepointRef(pb, context, tp, refCounter),
      conditions.getInstance(context, temporalContext, pb, refCounter, optimizeTimepoints = false)
    )
  }
}
