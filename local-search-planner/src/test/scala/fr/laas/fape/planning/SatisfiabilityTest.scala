package fr.laas.fape.planning

import fr.laas.fape.anml.model.AnmlProblem
import fr.laas.fape.anml.model.concrete.InstanceRef
import fr.laas.fape.constraints.meta.{CSP, Configuration}
import fr.laas.fape.constraints.meta.search.TreeSearch
import fr.laas.fape.constraints.meta.types.statics.TypedVariable
import fr.laas.fape.constraints.meta.variables.IntVariable
import fr.laas.fape.planning.events.{InitPlanner, PlanningHandler}
import org.scalatest.FunSuite

class SatisfiabilityTest extends FunSuite {

  test("Single sat/unsat (for debugging)") {
    testSat(12)
  }

  for(i <- Instances.satisfiables.indices) {
    test(s"satisfiability #${i+1}") {
      testSat(i+1)
    }
  }

  for(i <- Instances.unsatisfiables.indices) {
    test(s"unsatisfiability #${i+1}") {
      testUnsat(i+1)
    }
  }

  def testSat(i: Int) {
    val pb = Instances.satisfiables(i-1)
    println(pb)
    implicit val csp = plan(pb)
    assert(csp != null)
    assert(csp.isSolution, csp.report)

    // print important variables
    val vars = csp.constraints.all.flatMap(c => c.variables(csp))
      .collect{case v:IntVariable if v.ref.isEmpty || !v.ref.get.isInstanceOf[InstanceRef] => v}
      .toSet
    for(v <- vars) v match {
      case v: TypedVariable[_] => println(s"$v = ${v.dom}")
      case _ => println(s"$v = ${v.domain}")
    }
    println(s"end in ${csp.temporalHorizon.domain}")
    println(csp.getHandler(classOf[PlanningHandler]).report)
  }

  def testUnsat(i: Int) {
    val pb = Instances.unsatisfiables(i-1)
      println(pb)
      implicit val csp = plan(pb)
      if(csp != null) {
        println("\n -------- HISTORY --------\n")
        println(csp.log.history)
        println("\n -------- REPORT --------\n")
        println(csp.report)

        println(csp.getHandler(classOf[PlanningHandler]).report)
      }
      assert(csp == null)
  }

  def plan(pbString: String): CSP = {
    val pb = new AnmlProblem
    pb.extendWith(pbString)
    val csp = new CSP(Left(new Configuration(enforceTpAfterStart = false)))
    csp.addHandler(new PlanningHandler(csp, Left(pb)))
    csp.addEvent(InitPlanner)

    val searcher = new TreeSearch(List(csp))
    searcher.incrementalDeepeningSearch() match {
      case Left(solution) => solution
      case _ => null
    }
  }

}
