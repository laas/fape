package planstack.constraints.stnu

import org.scalatest.FunSuite

class ConcreteEDGForTesting extends EDG {
  val start = addVar()
  val end = addVar()
}

class EDGSqueezingSuite extends FunSuite {
  val A = 0
  val B = 1
  val C = 2
  val D = 3

  

  test("No Squeezing, same values") {
    val edg = new ConcreteEDGForTesting

    edg.addContingent(A, B, 10)
    edg.addContingent(B, A, -8)
    edg.addRequirement(A, B, 10)
    edg.addRequirement(B, A, -8)

    assert(!edg.squeezed)
  }

  test("No Squeezing, higher values") {
    val edg = new ConcreteEDGForTesting

    edg.addContingent(A, B, 10)
    edg.addContingent(B, A, -8)
    edg.addRequirement(A, B, 11)
    edg.addRequirement(B, A, -7)

    assert(!edg.squeezed)
  }

  test("Squeezing") {
    val edg = new ConcreteEDGForTesting

    edg.addContingent(A, B, 10)
    edg.addContingent(B, A, -8)
    edg.addRequirement(A, B, 9)
    edg.addRequirement(B, A, -8)

    assert(edg.squeezed)
  }

}
