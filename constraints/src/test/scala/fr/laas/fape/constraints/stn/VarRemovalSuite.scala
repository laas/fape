package fr.laas.fape.constraints.stn

import org.scalatest.FunSuite

class VarRemovalSuite extends FunSuite {

  for(stn <- Predef.getAllISTN[Nothing]) {
    test("["+stn.getClass.getSimpleName+"] Simple var count") {

      val a = stn.addVar()
      val b = stn.addVar()
      val c = stn.addVar()

      stn.enforceInterval(a, b, 10, 10)
      stn.enforceInterval(b, c, 10, 10)

      assert(stn.size == 5)
      assert(stn.earliestStart(c) == 20)
      stn.removeVar(b)
      assert(stn.size == 4)
      stn.checkConsistency()
      assert(stn.earliestStart(c) == 0)

      val d = stn.addVar()
      assert(d == b)
      assert(stn.size == 5)

    }
  }

}
