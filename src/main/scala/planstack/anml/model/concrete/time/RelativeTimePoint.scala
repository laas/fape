package planstack.anml.model.concrete.time

import planstack.anml.model.Context
import planstack.anml.model.abs.time.AbstractRelativeTimePoint

/** Refers to a virtual timepoint placed relatively to a concrete timepoint.
  *
  * For instance the virtual timepoint `start+10`, refers to a virtual timepoint placed 10 time units
  * after the start of the containing [[planstack.anml.model.concrete.TemporalInterval]]. It is used
  * in [[planstack.anml.model.concrete.time.TemporalAnnotation]] to represent the start and en time time-points of the
  * the annotation.
  *
  *@param timepoint Real time points to use for absolute positioning
  * @param delta Number of time units between `timepoint` and this relative timepoint. If `delta` is negative, the relative
  *              time points occurs before `timepoint`
  */
class RelativeTimePoint(val timepoint:TimepointRef, val delta:Int) {

  override def toString =
    if(delta == 0)
      timepoint.toString
    else if(delta >= 0)
      "%s+%s".format(timepoint, delta)
    else
      timepoint.toString + delta
}

object RelativeTimePoint {

  def apply(context:Context, abs:AbstractRelativeTimePoint) = {
    new RelativeTimePoint(TimepointRef(context, abs.timepoint), abs.delta)
  }
}