import org.scalatest.FunSuite
import planstack.anml.model.AnmlProblem
import planstack.anml.model.concrete.Factory

import planstack.anml.parser.AnmlParser._

class ActionTimepoint extends FunSuite {

  val dom =
    """
      | predicate do();
      | predicate done();
      |
      | action subDo() {
      |   [end] done := true;
      |
      | };
      |
      | action Do() {
      |   [all] do == false :-> true;
      |   [start+10,end-5] subDo();
      |   [all] contains done == true;
      | };
    """.stripMargin

  test("none") {

    val pb = new AnmlProblem(usesActionConditions = true)


    parseAll(anml, dom) match {
    case Success(res, _) => {
      println(res)
      pb.addAnmlBlocks(res)
      println(pb)
      val doo = Factory.getStandaloneAction(pb, pb.getAction("Do"))
//      println(doo.timepoints().mkString("\n"))
//      println(doo.rigids().mkString("\n"))
    }
    case x => {
      println(x)
//      throw new ANMLException("Problem")
    }
  }
  }
}
