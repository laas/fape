package fr.laas.fape.anml.model.abs.time

import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.model.LocalRef
import fr.laas.fape.anml.parser.TimepointRef
import fr.laas.fape.anml.parser


abstract class AbsTP {
  val genre = new TimepointType
}

case object TimeOrigin extends AbsTP
case object ContainerStart extends AbsTP {
  override def toString = "start"
}
case object ContainerEnd extends AbsTP {
  override def toString = "end"
}
case class IntervalStart(id :LocalRef) extends AbsTP {
  override def toString = s"start($id)"
}
case class IntervalEnd(id :LocalRef) extends AbsTP {
  override def toString = s"end($id)"
}
case class StandaloneTP(id: String) extends AbsTP {
//  require(id != "start" && id != "end" && !id.contains("(") && !id.contains(")"), "Invalid standalone tp: "+id)
}



object AbsTP {

  def apply(parsed:TimepointRef) = {
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