package planstack.anml.model.abs.time

import planstack.anml.model.LocalRef
import planstack.anml.parser







class AbstractTemporalAnnotation(val start:AbstractRelativeTimePoint, val end:AbstractRelativeTimePoint, val flag:String) {
  require(flag =="is" || flag == "contains")

  override def toString =
    if(flag == "is") "[%s, %s]".format(start, end)
    else "[%s, %s] contains".format(start, end)
}

object AbstractTemporalAnnotation {

  def apply(annot:parser.TemporalAnnotation) : AbstractTemporalAnnotation = {
    new AbstractTemporalAnnotation(AbstractRelativeTimePoint(annot.start), AbstractRelativeTimePoint(annot.end), annot.flag)
  }

  def apply(s:String, e:String) = {
    new AbstractTemporalAnnotation(
      new AbstractRelativeTimePoint(new AbstractTimepointRef(s,new LocalRef("")),0),
      new AbstractRelativeTimePoint(new AbstractTimepointRef(e,new LocalRef("")),0),
      "is")
  }
}
