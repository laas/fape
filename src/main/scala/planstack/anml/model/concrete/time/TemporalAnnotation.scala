package planstack.anml.model.concrete.time

import planstack.anml.model.Context
import planstack.anml.model.abs.time.AbstractTemporalAnnotation


class TemporalAnnotation(val start:RelativeTimePoint, val end:RelativeTimePoint) {

  override def toString = "[%s, %s]".format(start, end)
}

object TemporalAnnotation {

  def apply(context:Context, abs:AbstractTemporalAnnotation) =
    new TemporalAnnotation(
      RelativeTimePoint(context, abs.start),
      RelativeTimePoint(context, abs.end))

}