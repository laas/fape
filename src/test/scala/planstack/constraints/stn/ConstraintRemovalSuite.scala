package planstack.constraints.stn

import org.scalatest.FunSuite
import planstack.constraints.CSP
import scala.collection.JavaConversions._

class ConstraintRemovalSuite extends FunSuite {


  test("Removal by var IDs") {
    val stn = new STNIncBellmanFord()
    var clone = stn.cc()

    def checkCloneEqSTN {
      assert((0 to stn.size-1).forall(i => stn.forwardDist(i) == clone.forwardDist(i)))
      assert((0 to stn.size-1).forall(i => stn.backwardDist(i) == clone.backwardDist(i)))
    }
    val u = stn.addVar()
    val v = stn.addVar()
    val w = stn.addVar()



    clone = stn.cc()
    clone.checkConsistencyFromScratch()
    checkCloneEqSTN

    stn.addConstraint(u, v, 5)
    stn.addConstraint(u, v, -5)

    clone = stn.cc()
    clone.checkConsistencyFromScratch()
    checkCloneEqSTN


    clone = stn.cc()
    stn.addConstraint(u,w, 10)
    stn.removeConstraint(u, w)
    checkCloneEqSTN
  }

  test("Removal by constraint ID") {
    val stn = new STNIncBellmanFord[String]()

    val a = stn.addVar()
    val b = stn.addVar()
    val c = stn.addVar()

    stn.enforceInterval(stn.start, a, 0, 0)
    stn.enforceInterval(a, b, 1, 4)
    stn.enforceInterval(b, c, 1, 4)
    stn.addConstraintWithID(a, c, 5, "rm")
    stn.addConstraintWithID(c, a, -4, "rm")

    assert(stn.consistent)
    assert(stn.earliestStart(c) == 4)
    assert(stn.latestStart(c) == 5)

    stn.removeConstraintsWithID("rm")

    assert(stn.consistent)
    assert(stn.earliestStart(c) == 2)
    assert(stn.latestStart(c) == 8)

    stn.addConstraintWithID(c, a, -9, "rm2")
    assert(!stn.consistent)

    stn.removeConstraintsWithID("rm2")
    assert(stn.consistent)
    assert(stn.earliestStart(c) == 2)
    assert(stn.latestStart(c) == 8)
  }

  test("Removal by constraint ID of a constraint that was dominating an other.") {
    val stn = new STNIncBellmanFord[String]()

    stn.addConstraintWithID(stn.start, stn.end, 10, "first")
    stn.addConstraintWithID(stn.start, stn.end, 12, "first")
    stn.addConstraintWithID(stn.start, stn.end, 15, "second")

    assert(stn.latestStart(stn.end) == 10)
    stn.removeConstraintsWithID("first")
    assert(stn.latestStart(stn.end) == 15)
  }

  test("Removal with pending mixed constraints") {
    val csp = new CSP[String,String,String]
    csp.bindings.addPossibleValue(3)
    csp.bindings.addPossibleValue(1)
    csp.bindings.addPossibleValue(2)
    csp.bindings.AddIntVariable("d")

    csp.stn.recordTimePoint("u")
    csp.stn.recordTimePoint("v")
    csp.addMinDelayWithID("u","v","d", "myID")
    assert(csp.stn.GetEarliestStartTime("v") <= 1)
    csp.removeConstraintsWithID("myID")
    csp.bindings.restrictIntDomain("d", List[Integer](3))

    // even with d binded, the constraint should not be propagated since it was removed
    assert(csp.stn.GetEarliestStartTime("v") <= 1)
  }

}
