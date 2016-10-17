package fr.laas.fape.constraints.stnu

import fr.laas.fape.constraints.stnu.nilsson.EDG
import org.scalatest.FunSuite

class ConcreteEDGForTesting[ID] extends EDG[ID](checkCycles = true) {
  val start = addVar()
  val end = addVar()
}

class EDGSqueezingSuite extends FunSuite {

  test("No Squeezing, same values") {
    val edg = new ConcreteEDGForTesting

    val A = edg.addVar()
    val B = edg.addVar()

    edg.addContingent(A, B, 10)
    edg.addContingent(B, A, -8)
    edg.addRequirement(A, B, 10)
    edg.addRequirement(B, A, -8)

    assert(!edg.squeezed)
  }

  test("No Squeezing, higher values") {
    val edg = new ConcreteEDGForTesting

    val A = edg.addVar()
    val B = edg.addVar()

    edg.addContingent(A, B, 10)
    edg.addContingent(B, A, -8)
    edg.addRequirement(A, B, 11)
    edg.addRequirement(B, A, -7)

    assert(!edg.squeezed)
  }

  test("Squeezing") {
    val edg = new ConcreteEDGForTesting

    val A = edg.addVar()
    val B = edg.addVar()

    edg.addContingent(A, B, 10)
    edg.addContingent(B, A, -8)
    edg.addRequirement(A, B, 9)
    edg.addRequirement(B, A, -8)

    assert(edg.squeezed)
  }

}
