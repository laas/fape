package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.util.Assertion._
import org.scalatest.FunSuite

class AssertionLevelTest extends FunSuite {

  test("Print assertion levels") {
    print("Enabled assertion levels: ")
    assert1({print(" 1"); true}, "")
    assert2({print(" 2"); true}, "")
    assert3({print(" 3"); true}, "")
    assert4({print(" 4"); true}, "")
    println()
  }

  test("Assertion lazy evaluation") {

    var assertionLevel = 0
    assert1({assertionLevel = 1; true}, "")
    assert2({assertionLevel = 2; true}, "")
    assert3({assertionLevel = 3; true}, "")
    assert4({assertionLevel = 4; true}, "")
    assert(assertionLevel == DEBUG_LEVEL)
  }

}
