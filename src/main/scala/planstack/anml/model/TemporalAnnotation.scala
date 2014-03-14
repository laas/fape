package planstack.anml.model

import planstack.anml.{ANMLException, parser}

/** Reference to a the start or end timepoint of a temporal interval.
  *
  * Ex: the reference ("start", "action1") refers to the start timepoint of the action with identifier "action1"
  *     the reference ("end, "") refers to the end timepoint of the containing interval.
  *
  * @param extractor Either "start", "end", "GStart" or "GEnd"
  * @param id Local identifier of the action. If empty, the extractor is to be applied on the interval containing the event
  */
class AbstractTimepointRef(val extractor:String, val id:String) {
  require(Set("GStart","GEnd","start","end").contains(extractor))

  override def toString = (extractor, id) match {
    case ("GStart", _) => "GStart"
    case ("GEnd", _) => "GEnd"
    case (ext, "") => ext
    case (ext, ident) => "%s(%s)".format(ext, ident)
  }
}

object AbstractTimepointRef {

  def apply(parsed:parser.TimepointRef) = {
    parsed match {
      case parser.TimepointRef("", "") => new AbstractTimepointRef("GStart", "")
      case parser.TimepointRef("", _) => throw new ANMLException("Invalid timepoint reference: "+parsed)
      case parser.TimepointRef(extractor, id) => new AbstractTimepointRef(extractor, id)
    }
  }

  def apply(extractor:String) = new AbstractTimepointRef(extractor, "")
}


class RelativeTimePoint(val timepoint:AbstractTimepointRef, val delta:Int) {

  override def toString =
    if(delta == 0)
      timepoint.toString
    else if(delta >= 0)
      "%s+%s".format(timepoint, delta)
    else
      timepoint.toString + delta
}

object RelativeTimePoint {

  def apply(rtp:parser.RelativeTimepoint) = rtp.tp match {
    case None => new RelativeTimePoint(new AbstractTimepointRef("GStart", ""), rtp.delta)
    case Some(tpRef) => new RelativeTimePoint(AbstractTimepointRef(tpRef), rtp.delta)
  }
}


class TemporalAnnotation(val start:RelativeTimePoint, val end:RelativeTimePoint) {


  override def toString = "[%s, %s]".format(start, end)
}

object TemporalAnnotation {


  def apply(annot:parser.TemporalAnnotation) : TemporalAnnotation = {
    new TemporalAnnotation(RelativeTimePoint(annot.start), RelativeTimePoint(annot.end))
  }

  def apply(s:String, e:String) = {
    new TemporalAnnotation(
      new RelativeTimePoint(new AbstractTimepointRef(s,""),0),
      new RelativeTimePoint(new AbstractTimepointRef(e,""),0))
  }
}
