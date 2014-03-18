package planstack.anml.model.concrete.time

import planstack.anml.model.Context
import planstack.anml.model.abs.time.AbstractRelativeTimePoint


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