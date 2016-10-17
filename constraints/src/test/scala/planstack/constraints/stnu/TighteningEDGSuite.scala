package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.constraints.stnu.nilsson.{Conditional, Contingent, Requirement, STNULabel}
import planstack.graph.core.LabeledEdge

class TighteningEDGSuite extends FunSuite {
  type E = LabeledEdge[Int, STNULabel[Int]]
  val A = 0
  val B = 1
  val C = 2
  val D = 3

  val edg = new ConcreteEDGForTesting[Int]

  edg.addContingent(A, B, 10)
  edg.addContingent(B, A, -8)
  edg.addRequirement(A, B, 11)
  edg.addRequirement(B, A, -7)
  edg.addContingent(A, C, 10)

  edg.addConditional(A, B, C, 0)

  test("Tightening requirements") {
    assert(edg.tightens(new E(A, B, new Requirement(8))))
    assert(edg.tightens(new E(B, A, new Requirement(-30))))
    assert(edg.tightens(new E(C, A, new Requirement(9999))))

    assert(!edg.tightens(new E(A, B, new Requirement(12))))
    assert(!edg.tightens(new E(A, B, new Requirement(11))))
    assert(!edg.tightens(new E(B, A, new Requirement(-7))))
    assert(!edg.tightens(new E(B, A, new Requirement(30))))
  }

  test("Tightening contingents") {
    assert(edg.tightens(new E(C, A, new Contingent(-99))))
  }

  test("Tightening conditionals") {
    assert(edg.tightens(new E(A, B, new Conditional(C, -1))))
    assert(!edg.tightens(new E(A, B, new Conditional(C, 0))))
    assert(!edg.tightens(new E(A, B, new Conditional(C, 9))))

    assert(edg.tightens(new E(A, C, new Conditional(C, 1))))
    assert(edg.tightens(new E(C, B, new Conditional(C, 1))))
    assert(edg.tightens(new E(A, B, new Conditional(A, 1))))
  }

}
