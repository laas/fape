package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.variables.IntVariable
import org.scalatest.{BeforeAndAfter, FunSuite}

/**
  * Tests case that essentially use some previous tests with cloning operations in between
  */
class CSPCloneTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = null
  implicit var v1, v2, v3: IntVariable = _

  before {
    csp = new CSP
    v1 = csp.variable("v1", Set(1,2,3))
    v2 = csp.variable("v2", Set(1))
    v3 = csp.variable("v3", Set(1, 2))
  }

  test("Reification valid on equality [cloning]") {
    val c = v1 === v2
    val r = csp.reified(c)
    csp.propagate()
    assert(!r.isTrue)
    assert(!r.isFalse)

    csp.post(v1 === v2)
    csp = csp.clone
    csp.propagate()
    assert(r.isTrue)
  }

  test("Reification invalid on equality [cloning]") {
    val c = v1 === v3
    val r = csp.reified(c)
    csp = csp.clone
    csp.propagate()
    assert(!r.isTrue)
    assert(!r.isFalse)

    csp.post(v1 === v3)
    csp.propagate()
    assert(!r.isTrue)
    assert(!r.isFalse)
  }


  test("Simple STN with reification [cloning]") {

    val tp1 = csp.varStore.getTimepoint("first")
    val tp2 = csp.varStore.getTimepoint("second")
    val tp3 = csp.varStore.getTimepoint("third")

    val rei = csp.reified(tp3 < 2)
    val rei2 = csp.reified(tp1 < tp3)
    csp = csp.clone
    csp.propagate()
    csp = csp.clone
    csp.post(tp1 < tp2)
    csp = csp.clone
    csp.post(tp2 < tp3)
    csp = csp.clone
    csp.post(csp.temporalHorizon <= 100)

    csp.propagate()
    csp = csp.clone
    assert(rei.isFalse)
    assert(rei2.isTrue)
  }

}
