package fr.laas.fape.constraints.stnu.morris

import fr.laas.fape.constraints.stnu.morris.DCMorris.Edge
import DCMorris.{Edge, Lower, Req, Upper}

import scala.util.parsing.combinator.JavaTokenParsers


/** Parser for PDDL problems files */
object Parser extends JavaTokenParsers {
  def inWord: Parser[String] = """[a-zA-Z0-9][a-zA-Z0-9_\-]*""".r //ident

  def word: Parser[String] = inWord ^^ (w => w.toLowerCase)

  def id: Parser[Int] = decimalNumber ^^ (x => x.toInt)
  def delay: Parser[Int] = wholeNumber ^^ (x => x.toInt)

  def problem: Parser[(List[(String,Int)],List[Edge])] =
    "(define"~>timepoints~constraints<~")" ^^ { case tps~edges => (tps, edges) }

  def timepoints: Parser[List[(String,Int)]] = "(:timepoints"~>rep(timepoint)<~")"

  def timepoint: Parser[(String,Int)] = "("~>word~id<~")" ^^ { case typ~id => (typ, id)}

  def constraints: Parser[List[Edge]] = "(:constraints"~>rep(constraint)<~")"

  def constraint: Parser[Edge] =
    "(req"~>id~id~delay<~")" ^^ { case from~to~d => new Req(from, to, d, Set()) } |
      "(upper"~>id~id~delay~id<~")" ^^ { case from~to~d~lbl => new Upper(from, to, d, lbl, Set())} |
      "(lower"~>id~id~delay~id<~")" ^^ { case from~to~d~lbl => new Lower(from, to, d, lbl, Set())}
}