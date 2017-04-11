package fr.laas.fape.planning

import fr.laas.fape.anml.model.AnmlProblem
import fr.laas.fape.anml.model.concrete.InstanceRef
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.search.BinarySearch
import fr.laas.fape.constraints.meta.types.TypedVariable
import fr.laas.fape.constraints.meta.variables.IntVariable
import fr.laas.fape.planning.events.{InitPlanner, PlanningHandler}
import org.scalatest.FunSuite

class SatisfiabilityTest extends FunSuite {

  for(i <- Instances.satisfiables.indices) {
    test(s"satisfiability #${i+1}") {
      val pb = Instances.satisfiables(i)
      println(pb)
      implicit val csp = plan(pb)
      assert(csp != null, csp.log.history)

      // print important variables
      val vars = csp.constraints.all.flatMap(c => c.variables(csp))
        .collect{case v:IntVariable if v.ref.isEmpty || !v.ref.get.isInstanceOf[InstanceRef] => v}
        .toSet
      for(v <- vars) v match {
        case v: TypedVariable[_] => println(s"$v = ${v.dom}")
        case v => println(s"$v = ${v.domain}")
      }
    }
  }

  for(i <- Instances.unsatisfiables.indices) {
    test(s"unsatisfiability #${i+1}") {
      val pb = Instances.unsatisfiables(i)
      println(pb)
      implicit val csp = plan(pb)
      assert(csp == null)
    }
  }

  def plan(pbString: String): CSP = {
    val pb = new AnmlProblem
    pb.extendWith(pbString)
    val csp = new CSP
    csp.addHandler(new PlanningHandler(csp, Left(pb)))
    csp.addEvent(InitPlanner)

    BinarySearch.search(csp)
  }

}
