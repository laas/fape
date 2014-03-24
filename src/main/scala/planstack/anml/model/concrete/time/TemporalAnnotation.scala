package planstack.anml.model.concrete.time

import planstack.anml.model.Context
import planstack.anml.model.abs.time.AbstractTemporalAnnotation


/** Representation of an ANML temporal annotation such as [start, end+10].
  *
  * @param start Left part of the annotation.
  * @param end Right part of the annotation
  */
class TemporalAnnotation(val start:RelativeTimePoint, val end:RelativeTimePoint) {

  override def toString = "[%s, %s]".format(start, end)
}

object TemporalAnnotation {

  def apply(context:Context, abs:AbstractTemporalAnnotation) =
    new TemporalAnnotation(
      RelativeTimePoint(context, abs.start),
      RelativeTimePoint(context, abs.end))

}