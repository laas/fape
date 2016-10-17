package fr.laas.fape.constraints.stn

import org.scalatest.FunSuite

import scala.collection.JavaConversions._

class ConstraintRemovalSuite extends FunSuite {

  for(stn <- Predef.getAllISTN[String]) {

    test("[" + stn.getClass.getSimpleName + "] Simple Removal by constraint IDs") {
      var clone = stn.cc()

      def checkCloneEqSTN(stn1:CoreSTN[String], stn2:CoreSTN[String]) {
        assert(stn1.size == stn2.size)
        assert(stn1.consistent)
        assert(stn2.consistent)
//        stn1.events.foreach(i => println(""+i+" "+stn1.earliestStart(i)+" "+stn2.earliestStart(i)))
        assert(stn1.events.forall(i => stn1.earliestStart(i) == stn2.earliestStart(i)))
        assert(stn1.events.forall(i => stn1.latestStart(i) == stn2.latestStart(i)))
      }
      val u = stn.addVar()
      val v = stn.addVar()
      val w = stn.addVar()

      stn.addConstraint(stn.start, u, 0)
      stn.addConstraint(u, stn.start, 0)
      stn.checkConsistency()

      clone = stn.cc()
      clone.checkConsistencyFromScratch()
      checkCloneEqSTN(stn, clone)

      stn.addConstraint(u, v, 5)
      stn.addConstraint(v, u, -5)
      stn.checkConsistency()

      clone = stn.cc()
      clone.checkConsistencyFromScratch()
      checkCloneEqSTN(stn, clone)


      clone = stn.cc()
      stn.addConstraintWithID(u, w, 10, "a")
      stn.removeConstraintsWithID("a")
      checkCloneEqSTN(stn, clone)
    }
  }

  for(stn <- Predef.getAllISTN[String]) {

    test("[" + stn.getClass.getSimpleName + "] Removal by constraint ID") {

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
  }

  for(stn <- Predef.getAllISTN[String]) {

    test("["+stn.getClass.getSimpleName+ "] Removal by constraint ID of a constraint that was dominating an other.") {

      stn.addConstraintWithID(stn.start, stn.end, 10, "first")
      stn.addConstraintWithID(stn.start, stn.end, 12, "first")
      stn.addConstraintWithID(stn.start, stn.end, 15, "second")

      assert(stn.latestStart(stn.end) == 10)
      stn.removeConstraintsWithID("first")
      assert(stn.latestStart(stn.end) == 15)
    }
  }

  /*
  test("Removal with pending mixed constraints") {
    val csp = new MetaCSP[Ref,Ref,Ref]
    csp.bindings.addPossibleValue(3)
    csp.bindings.addPossibleValue(1)
    csp.bindings.addPossibleValue(2)
    csp.bindings.AddIntVariable("d")

    csp.stn.recordTimePoint("u")
    csp.stn.recordTimePoint("v")
    csp.addMinDelayWithID("u","v","d", "myID")
    assert(csp.stn.getEarliestStartTime("v") <= 1)
    csp.removeConstraintsWithID("myID")
    csp.bindings.restrictIntDomain("d", List[Integer](3))

    // even with d binded, the constraint should not be propagated since it was removed
    assert(csp.stn.getEarliestStartTime("v") <= 1)
  }
*/
}
