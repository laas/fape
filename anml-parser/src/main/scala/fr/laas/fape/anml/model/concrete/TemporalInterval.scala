package fr.laas.fape.anml.model.concrete

/**
 * A TemporalInterval is an interface used to describe any anml object having a start and end timepoints such as
 * [[planstack.anml.model.AnmlProblem]], [[Action]],
 * [[planstack.anml.model.concrete.statements.Statement]], ...
 *
 * It provides any class implementing it with a two time-points referring to the start and end of its temporal interval.
 */
trait TemporalInterval {

  /** Time-point referring to the start of the temporal interval. */
  val start :TPRef

  /** Time-point referring to the end of the temporal interval. */
  val end :TPRef

}
