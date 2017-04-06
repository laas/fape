package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.variables.Variable
import org.scalatest.{BeforeAndAfter, FunSuite}
import fr.laas.fape.constraints.meta.search.BinarySearch

class DisjunctiveConstraintTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = null
  var v1, v2, v3: Variable = null

  before {
    csp = new CSP
    v1 = csp.addVariable("v1", Set(1,2,3))
    v2 = csp.addVariable("v2", Set(2))
    v3 = csp.addVariable("v3", Set(1, 2, 3))
  }

  test("Disjunction constraint enforced by search") {
    csp.post(v2 =!= v1 || v2 =!= v3)
    csp.propagate()
    val res = BinarySearch.search(csp)
    assert(res != null)
    println(res.report)
    assert(res.isSolution)
  }
}
