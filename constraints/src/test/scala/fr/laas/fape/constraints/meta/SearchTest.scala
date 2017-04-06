package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.search.BinarySearch
import fr.laas.fape.constraints.meta.variables.IntVariable
import org.scalatest.{BeforeAndAfter, FunSuite}

class SearchTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = _
  implicit var v1, v2, v3: IntVariable = _

  before {
    csp = new CSP
    v1 = csp.variable("v1", Set(1, 2, 3))
    v2 = csp.variable("v2", Set(1, 2))
    v3 = csp.variable("v3", Set(1, 2))
  }

  test("Simple successful search") {
    csp.post(v1 =!= v2)
    csp.post(v2 =!= v3)
    csp.post(v1 =!= v3)

    val ret = BinarySearch.search(csp)
    assert(ret != null)
    println(ret.report)
  }

  test("Simple impossible search") {
    csp.post(v1 =!= 3)
    csp.post(v1 =!= v2)
    csp.post(v2 =!= v3)
    csp.post(v1 =!= v3)

    val ret = BinarySearch.search(csp)
    assert(ret == null)
  }
}
