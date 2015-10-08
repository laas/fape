package planstack.anml.model.abs.time

import planstack.anml.model.{LActRef, LocalRef}
import planstack.anml.{ANMLException, parser}

/** Reference to a the start or end timepoint of a temporal interval.
  *
  * Ex: the reference ("start", "action1") refers to the start timepoint of the action with identifier "action1"
  *     the reference ("end, "") refers to the end timepoint of the containing interval.
  *
  * @param extractor Either "start", "end", "GStart" or "GEnd"
  * @param id Local identifier of the action. If empty, the extractor is to be applied on the interval containing the event
  */
case class AbsTP(extractor:String, id:LocalRef) {
  require(Set("GStart","GEnd","start","end").contains(extractor))

  override def toString = (extractor, id) match {
    case ("GStart", _) => "GStart"
    case ("GEnd", _) => "GEnd"
    case (ext, ident) =>
      if(ident.isEmpty) ext
      else "%s(%s)".format(ext, ident)
  }

  override def equals(obj: Any) = obj match {
    case AbsTP("GStart", _) => extractor == "GStart"
    case AbsTP("GEnd", _) => extractor == "GEnd"
    case AbsTP(oExtr, oId) => extractor == oExtr && oId == id
    case _ => false
  }

  override def hashCode =
    if(extractor == "GStart" || extractor == "GEnd")
      extractor.hashCode
    else
      extractor.hashCode + id.hashCode
}

object AbsTP {

  def apply(parsed:parser.TimepointRef) = {
    parsed match {
      case parser.TimepointRef("", "") => new AbsTP("GStart", new LocalRef(""))
      case parser.TimepointRef("", _) => throw new ANMLException("Invalid timepoint reference: "+parsed)
      case parser.TimepointRef(extractor, "") => new AbsTP(extractor, new LocalRef(""))
      case parser.TimepointRef(extractor, id) => new AbsTP(extractor, new LocalRef(id))
    }
  }

  def apply(extractor:String) = new AbsTP(extractor, new LActRef(""))
}