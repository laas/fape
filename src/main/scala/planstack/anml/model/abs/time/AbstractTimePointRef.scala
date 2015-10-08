package planstack.anml.model.abs.time

import planstack.anml.model.LocalRef
import planstack.anml.{ANMLException, parser}



abstract class AbsTP

case object TimeOrigin extends AbsTP
case object ContainerStart extends AbsTP
case object ContainerEnd extends AbsTP
case class IntervalStart(id :LocalRef) extends AbsTP
case class IntervalEnd(id :LocalRef) extends AbsTP



object AbsTP {

  def apply(parsed:parser.TimepointRef) = {
    parsed match {
      case parser.TimepointRef("", "") => TimeOrigin
      case parser.TimepointRef("", _) => throw new ANMLException("Invalid timepoint reference: "+parsed)
      case parser.TimepointRef("start", "") => ContainerStart
      case parser.TimepointRef("end", "") => ContainerEnd
      case parser.TimepointRef("start", id) => IntervalStart(new LocalRef(id))
      case parser.TimepointRef("end", id) => IntervalEnd(new LocalRef(id))
    }
  }
}