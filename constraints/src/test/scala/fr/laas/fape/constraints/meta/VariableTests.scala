package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.variables.{Variable, VariableStore}
import org.scalatest.FunSuite

class VariableTests extends FunSuite {

  test("Hash and equals") {
    assert(new Variable(0) == new Variable(0))
    assert(new Variable(0) != new Variable(1))
  }

  test("creation through store") {
    val store = new VariableStore
    val v1 = store.getVariable()
    val v2 = store.getVariable()
    assert(v1 != v2)
  }

  test("creation by ref") {
    val store = new VariableStore
    val v1 = store.getVariableForRef("ref1")
    val v2 = store.getVariableForRef("ref2")
    val v1_other = store.getVariableForRef("ref1")

    assert(v1 == v1_other)
    assert(v1 != v2)
  }

}
