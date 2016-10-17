package fr.laas.fape.constraints.stn

import org.scalatest.FunSuite

class STNConsistency extends FunSuite {

  for(stn <- Predef.getAllISTN[Int]) {

    test("["+stn.getClass.getSimpleName+"] STN inconsistency: ") {

      val A = stn.addVar()
      val B = stn.addVar()
      val C = stn.addVar()
      val D = stn.addVar()
      val E = stn.addVar()
      val F = stn.addVar()
      val G = stn.addVar()

      stn.enforceInterval(A, B, 3, 7)
      stn.enforceInterval(B, C, 5, 9)
      stn.enforceInterval(C, D, 30, 40)
      stn.enforceInterval(F, E, 3, 7)
      stn.enforceInterval(G, F, 4, 9)
      stn.enforceInterval(A, G, 3, 7)

      assert(stn.consistent)

      stn.addConstraint(E, D, 5)

      assert(!stn.consistent)

    }
  }


}
