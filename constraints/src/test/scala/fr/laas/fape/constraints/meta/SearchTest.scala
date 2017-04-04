package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.search.BinarySearch
import fr.laas.fape.constraints.meta.variables.Variable
import org.scalatest.{BeforeAndAfter, FunSuite}

class SearchTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = _
  implicit var v1, v2, v3: Variable = _

  before {
    csp = new CSP
    v1 = csp.addVariable("v1", Set(1, 2, 3))
    v2 = csp.addVariable("v2", Set(1, 2))
    v3 = csp.addVariable("v3", Set(1, 2))
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
