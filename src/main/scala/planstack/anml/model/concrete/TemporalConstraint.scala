package planstack.anml.model.concrete

import planstack.anml.model.{GlobalRef, AbstractTimepointRef, Context}
import planstack.anml.model.abs.{AbstractTemporalConstraint}

class TimepointRef(extractor:String, id:GlobalRef) {
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

  def apply(context:Context, abs:AbstractTimepointRef) =
    new TimepointRef(abs.extractor, context.getActionID(abs.id))
}

class TemporalConstraint(val tp1:TimepointRef, val op:String, val tp2:TimepointRef, val plus:Integer) {
  require(op == "<" || op == "=")

  override def toString = "%s %s %s + %s".format(tp1, op, tp2, plus)
}

object TemporalConstraint {

  def apply(context:Context, abs:AbstractTemporalConstraint) = {
    new TemporalConstraint(TimepointRef(context, abs.tp1), abs.op, TimepointRef(context, abs.tp2), abs.plus)
  }
}