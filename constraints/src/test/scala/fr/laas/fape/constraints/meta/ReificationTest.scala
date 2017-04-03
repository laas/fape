package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.variables.Variable
import org.scalatest.{BeforeAndAfter, FunSuite}

class ReificationTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = _
  implicit var v1, v2, v3: Variable = _

  before {
    csp = new CSP
    v1 = csp.addVariable("v1", Set(1,2,3))
    v2 = csp.addVariable("v2", Set(1))
    v3 = csp.addVariable("v3", Set(1, 2))
  }

  test("Reification valid on equality") {
    val c = v1 === v2
    val r = csp.reified(c)
    csp.propagate()
    assert(!r.isTrue)
    assert(!r.isFalse)

    csp.post(v1 === v2)
    csp.propagate()
    assert(r.isTrue)
  }

  test("Reification invalid on equality") {
    val c = v1 === v3
    val r = csp.reified(c)
    csp.propagate()
    assert(!r.isTrue)
    assert(!r.isFalse)

    csp.post(v1 === v3)
    csp.propagate()
    assert(!r.isTrue)
    assert(!r.isFalse)
  }

}
