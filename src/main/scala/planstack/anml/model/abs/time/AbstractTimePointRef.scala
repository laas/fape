package planstack.anml.model.abs.time

import planstack.anml.parser
import planstack.anml.model.{LocalRef, LActRef}
import planstack.anml.ANMLException

/** Reference to a the start or end timepoint of a temporal interval.
  *
  * Ex: the reference ("start", "action1") refers to the start timepoint of the action with identifier "action1"
  *     the reference ("end, "") refers to the end timepoint of the containing interval.
  *
  * @param extractor Either "start", "end", "GStart" or "GEnd"
  * @param id Local identifier of the action. If empty, the extractor is to be applied on the interval containing the event
  */
class AbstractTimepointRef(val extractor:String, val id:LocalRef) {
  require(Set("GStart","GEnd","start","end").contains(extractor))

  override def toString = (extractor, id) match {
    case ("GStart", _) => "GStart"
    case ("GEnd", _) => "GEnd"
    case (ext, ident) =>
      if(ident.isEmpty) ext
      else "%s(%s)".format(ext, ident)
  }
}

object AbstractTimepointRef {

  def apply(parsed:parser.TimepointRef) = {
    parsed match {
      case parser.TimepointRef("", "") => new AbstractTimepointRef("GStart", new LocalRef(""))
      case parser.TimepointRef("", _) => throw new ANMLException("Invalid timepoint reference: "+parsed)
      case parser.TimepointRef(extractor, "") => new AbstractTimepointRef(extractor, new LocalRef(""))
      case parser.TimepointRef(extractor, id) => new AbstractTimepointRef(extractor, new LActRef(id))
    }
  }

  def apply(extractor:String) = new AbstractTimepointRef(extractor, new LActRef(""))
}