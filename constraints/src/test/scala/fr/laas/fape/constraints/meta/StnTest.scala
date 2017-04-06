package fr.laas.fape.constraints.meta

import org.scalatest.{BeforeAndAfter, FunSuite}

class StnTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = null

  before {
    csp = new CSP
  }


  test("Simple STN with reification") {

    val tp1 = csp.varStore.getTimepoint("first")
    val tp2 = csp.varStore.getTimepoint("second")
    val tp3 = csp.varStore.getTimepoint("third")

    val rei = csp.reified(tp3 < 2)
    val rei2 = csp.reified(tp1 < tp3)
    csp.propagate()
    csp.post(tp1 < tp2)
    csp.post(tp2 < tp3)
    csp.post(csp.temporalHorizon <= 100)

    csp.propagate()

    assert(rei.constraint.isViolated)
    assert(rei2.constraint.isSatisfied)
    assert(rei.isFalse)
    assert(rei2.isTrue)
  }

  test("Very Simple STN with reification") {

//    val tp1 = csp.varStore.getTimepoint("first")

    val rei = csp.reified(csp.temporalHorizon < 2)
    csp.propagate()
    csp.post(csp.temporalHorizon < 2)
    csp.propagate()

    assert(rei.constraint.isSatisfied)
    assert(rei.isTrue)
  }
}
