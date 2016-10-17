package fr.laas.fape.constraints.stnu

import org.scalatest.FunSuite

class VirtualTPSuite extends FunSuite {
/*
  for(stn <- getAllSTNUManager[IRef,Int]) {
    test("[" + stn.getClass.getSimpleName + "] constraints are enforced") {
      stn.recordTimePointAsStart(1)
      stn.addControllableTimePoint(2)
      stn.addVirtualTimePoint(3, 2, 5)
      stn.enforceConstraint(1, 3, 10, 10)

      assert(stn.getEarliestStartTime(1) == 0)
      assert(stn.getLatestStartTime(1) == 0)
      assert(stn.getEarliestStartTime(3) == 10)
      assert(stn.getLatestStartTime(3) == 10)
      assert(stn.getEarliestStartTime(2) == 15)
      assert(stn.getLatestStartTime(2) == 15)
      assert(stn.isConsistent())

      stn.addPendingVirtualTimePoint(4)
      stn.addControllableTimePoint(5)
      stn.addControllableTimePoint(6)
      stn.enforceConstraint(4, 5, 10, 10)
      stn.enforceConstraint(5, 6, 10, 10)

      assert(stn.getEarliestStartTime(6) == 10)

      stn.setVirtualTimePoint(4, 1, 0)

      assert(stn.getEarliestStartTime(6) == 20)
    }
  }
  */
}
