package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.constraints.stn.{STNIncBellmanFord, ISTN}

class STNConsistency extends FunSuite {

  for(stn <- List[ISTN[Int]](new STNIncBellmanFord[Int](), new FastIDC[Int])) {

    // example from `Incremental Dynamic Controllability Revisited` fig. 3
    test("STN inconsistency: "+stn.getClass.getName) {

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
