package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.constraints.ExtensionConstraint
import fr.laas.fape.constraints.meta.domains.ExtensionDomain
import fr.laas.fape.constraints.meta.variables.IntVariable
import org.scalatest.{BeforeAndAfter, FunSuite}

class ExtensionConstraintTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = _
  implicit var v1, v2, v3: IntVariable = _
  var extDomain: ExtensionDomain = _

  before {
    csp = new CSP
    v1 = csp.variable("v1", Set(1,2,3))
    v2 = csp.variable("v2", Set(1))
    v3 = csp.variable("v3", Set(1, 2))
    extDomain = new ExtensionDomain(3)
    extDomain.addTuple(List(1, 1, 1))
    extDomain.addTuple(List(3, 1, 1))
  }

  test("Extension constraint") {
    csp.post(new ExtensionConstraint(List(v1, v2, v3), extDomain))
    csp.propagate()

    assert(v1.domain.contains(1))
    assert(v1.domain.contains(3))
    assert(!v1.domain.contains(2))
    assert(v2.domain.contains(1))
    assert(v3.domain.contains(1))
    assert(!v3.domain.contains(2))
    assert(!v3.domain.contains(3))
  }

}
