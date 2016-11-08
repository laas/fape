package fr.laas.fape.constraints.stnu.parser

import fr.laas.fape.anml.model.abs.time.TimepointTypeEnum._
import fr.laas.fape.anml.model.concrete.{ContingentConstraint, MinDelayConstraint, TPRef, TemporalConstraint}
import fr.laas.fape.anml.pending.IntExpression

import scala.util.parsing.combinator.JavaTokenParsers
import scala.collection.mutable

class STNUParser extends JavaTokenParsers {
  private val timepointsRecord = mutable.Map[Int,TPRef]()
  private var optStart: Option[TPRef] = None
  private var optEnd: Option[TPRef] = None

  def inWord: Parser[String] = """[a-zA-Z0-9][a-zA-Z0-9_\-]*""".r //ident

  def word: Parser[String] = inWord ^^ (w => w.toLowerCase)

  def id: Parser[Int] = decimalNumber ^^ (x => x.toInt)
  def delay: Parser[Int] = wholeNumber ^^ (x => x.toInt)

  def problem: Parser[(List[TPRef],List[TemporalConstraint],Option[TPRef],Option[TPRef])] =
    "(define"~>timepoints~constraints<~")" ^^ { case tps~edges => (tps, edges, optStart, optEnd) }

  def timepoints: Parser[List[TPRef]] = "(:timepoints"~>rep(timepoint)<~")" ^^ {
    case tps =>
      tps.foreach(tp => timepointsRecord.put(tp.id, tp))
      tps
  }

  def timepoint: Parser[TPRef] =
    "("~>word~id<~")" ^^ { case typ~id => getTPRef(typ, id) }

  private def getTPRef(typ: String, id: Int): TPRef = {
    val tp = new TPRef(id)
    typ match {
      case "start" => tp.genre.setType(DISPATCHABLE); optStart = Some(tp)
      case "end" => tp.genre.setType(DISPATCHABLE); optEnd = Some(tp)
      case "dispatchable" => tp.genre.setType(DISPATCHABLE)
      case "structural" => tp.genre.setType(STRUCTURAL)
      case "contingent" => tp.genre.setType(CONTINGENT)
      case x => sys.error("Unrecognized timepoint type: "+x)
    }
    tp
  }

  def constraints: Parser[List[TemporalConstraint]] = "(:constraints"~>rep(constraint)<~")"

  def constraint: Parser[TemporalConstraint] =
    "(min-delay"~>id~id~delay<~")" ^^
      { case from~to~d => new MinDelayConstraint(timepointsRecord(from), timepointsRecord(to), IntExpression.lit(d)) } |
    "("~"contingent"~>id~id~delay~delay<~")" ^^
      { case from~to~min~max => new ContingentConstraint(timepointsRecord(from),timepointsRecord(to), IntExpression.lit(min), IntExpression.lit(max))}
}