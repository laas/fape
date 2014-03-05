package planstack.anml.model

import planstack.anml.parser

class RelativeTimePoint(val timepoint:String, val delta:Int) {
  assert(Set("", "start", "end").contains(timepoint))

  override def toString =
    if(timepoint.isEmpty)
      delta.toString
    else if(delta == 0)
      timepoint
    else if(delta >= 0)
      "%s+%s".format(timepoint, delta)
    else
      timepoint+delta
}

object RelativeTimePoint {

  def apply(rtp:parser.RelativeTimepoint) = new RelativeTimePoint(rtp.tp, rtp.delta)
}

class TemporalAnnotation(val start:RelativeTimePoint, val end:RelativeTimePoint) {


  override def toString = "[%s, %s]".format(start, end)
}

object TemporalAnnotation {

  def apply(annot:parser.TemporalAnnotation) : TemporalAnnotation = {
    new TemporalAnnotation(RelativeTimePoint(annot.start), RelativeTimePoint(annot.end))
  }
}
