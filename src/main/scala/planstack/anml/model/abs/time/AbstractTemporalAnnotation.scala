package planstack.anml.model.abs.time

import planstack.anml.{ANMLException, parser}
import planstack.anml.model.LocalRef







class AbstractTemporalAnnotation(val start:AbstractRelativeTimePoint, val end:AbstractRelativeTimePoint) {

  override def toString = "[%s, %s]".format(start, end)
}

object AbstractTemporalAnnotation {

  def apply(annot:parser.TemporalAnnotation) : AbstractTemporalAnnotation = {
    new AbstractTemporalAnnotation(AbstractRelativeTimePoint(annot.start), AbstractRelativeTimePoint(annot.end))
  }

  def apply(s:String, e:String) = {
    new AbstractTemporalAnnotation(
      new AbstractRelativeTimePoint(new AbstractTimepointRef(s,new LocalRef("")),0),
      new AbstractRelativeTimePoint(new AbstractTimepointRef(e,new LocalRef("")),0))
  }
}
