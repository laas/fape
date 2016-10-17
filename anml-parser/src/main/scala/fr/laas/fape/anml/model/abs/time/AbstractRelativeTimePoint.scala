package fr.laas.fape.anml.model.abs.time

import fr.laas.fape.anml.parser.RelativeTimepoint

case class AbstractRelativeTimePoint(timepoint:AbsTP, delta:Int) {

 override def toString =
   if(delta == 0)
     timepoint.toString
   else if(delta >= 0)
     "%s+%s".format(timepoint, delta)
   else
     timepoint.toString + delta
}


object AbstractRelativeTimePoint {

  def apply(rtp:RelativeTimepoint) = rtp.tp match {
    case None => new AbstractRelativeTimePoint(TimeOrigin, rtp.delta)
    case Some(tpRef) => new AbstractRelativeTimePoint(AbsTP(tpRef), rtp.delta)
  }
}
