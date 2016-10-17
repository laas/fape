package fr.laas.fape.constraints.stnu

import fr.laas.fape.constraints.stn.Predef
import org.scalatest.FunSuite
import Predef._

class UncontrollableAction extends FunSuite {

  for(idc <- getAllISTNU[String]) {
    test("[" + idc.getClass.getSimpleName + "] Uncontrollable action") {

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
