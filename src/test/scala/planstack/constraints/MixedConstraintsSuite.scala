package planstack.constraints

import org.scalatest.FunSuite

class MixedConstraintsSuite extends FunSuite {

  test("isPropagated") {
    val csp = new CSP[String,String,String]()

    val iDomain = new java.util.LinkedList[Integer]
    for(i <- 1 until 5)
      iDomain.add(i)
    csp.bindings.AddIntVariable("d", iDomain)

    csp.stn.recordTimePoint("A")
    csp.stn.recordTimePoint("B")

    csp.addMaxDelay("A","B","d")

    assert(csp.varsToConstraints.nonEmpty)

    iDomain.removeFirst()
    csp.bindings.restrictIntDomain("d",iDomain)
    assert(csp.varsToConstraints.nonEmpty)

    iDomain.remove()
    csp.bindings.restrictIntDomain("d",iDomain)
    assert(csp.varsToConstraints.nonEmpty)

    iDomain.remove()
    csp.bindings.restrictIntDomain("d",iDomain)
    assert(csp.varsToConstraints.isEmpty)

    assert(csp.stn.IsConsistent())
    csp.stn.EnforceMinDelay("A","B",2)
    assert(csp.stn.IsConsistent())
    csp.stn.EnforceMinDelay("A","B",4)
    assert(csp.stn.IsConsistent())
    csp.stn.EnforceMinDelay("A","B",5)
    assert(!csp.stn.IsConsistent())
  }

  test("is Propagated Right Away On Singleton Domain (Max)") {
    val csp = new CSP[String,String,String]()

    val iDomain = new java.util.LinkedList[Integer]
    iDomain.add(4)
    csp.bindings.AddIntVariable("d", iDomain)

    csp.stn.recordTimePoint("A")
    csp.stn.recordTimePoint("B")

    csp.addMaxDelay("A","B","d")
    // should be propagated already
    assert(csp.varsToConstraints.isEmpty)

    csp.stn.EnforceMinDelay("A","B",4)
    assert(csp.stn.IsConsistent())
    csp.stn.EnforceMinDelay("A","B",5)
    assert(!csp.stn.IsConsistent())
  }

  test("is Propagated Right Away On Singleton Domain (Min)") {
    val csp = new CSP[String,String,String]()

    val iDomain = new java.util.LinkedList[Integer]
    iDomain.add(4)
    csp.bindings.AddIntVariable("d", iDomain)

    csp.stn.recordTimePoint("A")
    csp.stn.recordTimePoint("B")

    csp.addMinDelay("A","B","d")
    // should be propagated already
    assert(csp.varsToConstraints.isEmpty)

    csp.stn.EnforceMaxDelay("A","B",4)
    assert(csp.stn.IsConsistent())
    csp.stn.EnforceMaxDelay("A","B",3)
    assert(!csp.stn.IsConsistent())
  }


}
