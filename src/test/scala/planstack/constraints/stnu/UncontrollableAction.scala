package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.constraints.stn.Predef._

class UncontrollableAction extends FunSuite {

  for(idc <- getAllISTNU[String]) {
    test("Uncontrollable action") {

      val start = idc.addVar()
      val end = idc.addVar()
      val event = idc.addVar()

      idc.addContingent(start, end, 50, 60)
      idc.enforceInterval(event, end, 5, 15)

      assert(idc.consistent)

      idc.enforceInterval(event, end, 6, 15)

      assert(!idc.consistent)
    }
  }

}
