package planstack.anml.model.abs.time

import planstack.anml.model.{LocalRef, LActRef}
import planstack.anml.parser

class AbstractRelativeTimePoint(val timepoint:AbstractTimepointRef, val delta:Int) {

 override def toString =
   if(delta == 0)
     timepoint.toString
   else if(delta >= 0)
     "%s+%s".format(timepoint, delta)
   else
     timepoint.toString + delta
}


object AbstractRelativeTimePoint {

  def apply(rtp:parser.RelativeTimepoint) = rtp.tp match {
    case None => new AbstractRelativeTimePoint(new AbstractTimepointRef("GStart", new LocalRef("")), rtp.delta)
    case Some(tpRef) => new AbstractRelativeTimePoint(AbstractTimepointRef(tpRef), rtp.delta)
  }
}
