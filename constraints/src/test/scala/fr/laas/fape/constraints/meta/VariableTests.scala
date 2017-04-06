package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.variables.{IntVariable, VariableStore}
import org.scalatest.{BeforeAndAfter, FunSuite}

class VariableTests extends FunSuite with BeforeAndAfter {

  var csp: CSP = null
  var store: VariableStore = null

  before {
    csp = new CSP
    store = csp.varStore
  }

  test("creation by ref") {
    val v1 = csp.variable("ref1", Set(1))
    val v2 = csp.variable("ref2", Set(1))
    val v1_other = store.getVariable("ref1")

    assert(v1 == v1_other)
    assert(v1 != v2)
  }

}
