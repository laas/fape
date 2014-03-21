package planstack.anml.model.concrete.time

import planstack.anml.model.{LocalRef, LActRef, Context}
import planstack.anml.model.abs.time.AbstractTimepointRef
import planstack.anml.ANMLException
import planstack.anml.model.concrete.{EmptyGlobalRef, GlobalRef}

class TimepointRef(val extractor:String, val id:GlobalRef) {
  require(Set("GStart","GEnd","start","end").contains(extractor))

  override def toString = (extractor, id) match {
  case ("GStart", _) => "GStart"
  case ("GEnd", _) => "GEnd"
  case (ext, ident) =>
  if(ident.isEmpty) ext
  else "%s(%s)".format(ext, ident)
  }
}

object TimepointRef {

  def apply(context:Context, abs:AbstractTimepointRef) = {
    abs.id match {
      case actID:LActRef => new TimepointRef(abs.extractor, context.getActionID(actID))
      case loc:LocalRef if loc.isEmpty => new TimepointRef(abs.extractor, EmptyGlobalRef)
      case _ => throw new ANMLException("Unable to extract ID from context: "+abs.id)
    }
  }
}
