package fr.laas.fape.anml

import fr.laas.fape.anml.parser.AnmlParser
import AnmlParser._
import fr.laas.fape.anml.model.AnmlProblem
import fr.laas.fape.anml.model.concrete.Action

object Main extends App {


  val arg = ""
  println("input : "+ arg)
  println(parseAll(temporalStatements, "[all] x.v\n == g :-> q;"))

  //val block = "action Move(Robot r, Loc a, Loc b){[end] location(r) == a;}; [all] x.v == g;"
//  val block = ":decomposition{ a(), b(i) };"
//  parseAll(decomposition, block) match {
//    case Success(res, _) => println(res)
//    case x => println("Failure: "+x)
//  }

  val pb = new AnmlProblem()
  pb.extendWithAnmlFile("resources/text.anml")

//  val ref = new AbstractActionRef("Move", List("R0", "L0", "L1"), "")
//  val act = Action(pb, ref, DummyVariableFactory)

//  val ref = new AbstractActionRef("Transport", List("R0", "sdjqsdqsd_", "L1", "L0"), "")
//  val act = Action(pb, ref)
  val act = Action.getNewStandaloneAction(pb, "Transport", pb.refCounter)
  println(act)

}
