package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.variables.{Variable, VariableSeq}
import org.scalatest.{BeforeAndAfter, FunSuite}

class VariableSeqTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = _
  implicit var v1, v2, v3, w1, w2, w3: Variable = _

  before {
    csp = new CSP
    v1 = csp.addVariable("v1", Set(1,2,3))
    v2 = csp.addVariable("v2", Set(2))
    v3 = csp.addVariable("v3", Set(1, 3))

    w1 = csp.addVariable("w1", Set(1))
    w2 = csp.addVariable("w2", Set(2, 3))
    w3 = csp.addVariable("w3", Set(2, 3))
  }

  test("VariableSeq equality") {
    val vs1 = new VariableSeq(List(v1, v2, v3))
    val ws2 = new VariableSeq(List(w1, w2, w3))

    csp.post(vs1 === ws2)
    csp.propagate()
    assert(v1.value == 1)
    assert(v2.value == 2)
    assert(v3.value == 3)
    assert(w1.value == 1)
    assert(w2.value == 2)
    assert(w3.value == 3)
  }

  test("VariableSeq inequality") {
    val vs1 = new VariableSeq(List(v1, v2, v3))
    val ws2 = new VariableSeq(List(w1, w2, w3))

    csp.post(vs1 =!= ws2)
    csp.propagate()
    assert(List(v1, v3, w2, w3).forall(!_.isBound))

    csp.bind(v1, 1)
    csp.propagate()
    assert(List(v3, w2, w3).forall(!_.isBound))

    csp.bind(w2, 2)
    csp.bind(w3, 3)
    csp.propagate()
    assert(v3.value == 1)
    print(csp.report)
  }


}
