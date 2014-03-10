package planstack.anml

import scala.util.parsing.combinator._
import planstack.anml.parser.AnmlParser._
import java.io.FileReader
import planstack.anml.model.{DummyVariableFactory, AbstractActionRef, AnmlProblem}
import planstack.anml.model.concrete.{Decomposition, Action}

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

  val pb = new AnmlProblem


  parseAll(anml, new FileReader("resources/test.anml")) match {
    case Success(res, _) => {
//      println(res)
      pb.addAnmlBlocks(res)
    }
    case x => {
      println(x)
      throw new ANMLException("Problem")
    }
  }

//  val ref = new AbstractActionRef("Move", List("R0", "L0", "L1"), "")
//  val act = Action(pb, ref, DummyVariableFactory)

  val ref = new AbstractActionRef("Transport", List("R0", "I0", "L1", "L0"), "")
  val act = Action(pb, ref, DummyVariableFactory)

  val decs = act.decompositions.map(Decomposition(pb, act, _, DummyVariableFactory))
  println(act)

}
