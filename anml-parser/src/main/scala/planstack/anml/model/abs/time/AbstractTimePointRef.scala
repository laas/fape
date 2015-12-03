package planstack.anml.model.abs.time

import planstack.anml.model.LocalRef
import planstack.anml.{ANMLException, parser}



abstract class AbsTP

case object TimeOrigin extends AbsTP
case object ContainerStart extends AbsTP
case object ContainerEnd extends AbsTP
case class IntervalStart(id :LocalRef) extends AbsTP
case class IntervalEnd(id :LocalRef) extends AbsTP
case class StandaloneTP(id: String) extends AbsTP {
  require(id != "start" && id != "end" && !id.contains("(") && !id.contains(")"))
}



object AbsTP {

  def apply(parsed:parser.TimepointRef) = {
    parsed match {
      case parser.Timepoint("") => TimeOrigin
      case parser.Timepoint("start") => ContainerStart
      case parser.Timepoint("end") => ContainerEnd
      case parser.Timepoint(name) => StandaloneTP(name)
      case parser.ExtractedTimepoint("start", id) => IntervalStart(new LocalRef(id))
      case parser.ExtractedTimepoint("end", id) => IntervalEnd(new LocalRef(id))
      case x => throw new ANMLException("No match for timepoint: "+x)
    }
  }
}