package fr.laas.fape.anml.model.concrete

import fr.laas.fape.anml.model.concrete.time.TimepointRef
import fr.laas.fape.anml.model.{AnmlProblem, ChronicleContainer, Context}
import fr.laas.fape.anml.model.abs.AbstractChronicle
import fr.laas.fape.anml.model.abs.time.AbsTP

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
