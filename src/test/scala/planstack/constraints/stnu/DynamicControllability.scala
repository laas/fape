package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.graph.core.LabeledEdge
import planstack.graph.printers.NodeEdgePrinter
import planstack.constraints.stn.Predef._


class DynamicControllability extends FunSuite {

  for(idc <- getAllISTNU[String]) {

    // example from `Incremental Dynamic Controllability Revisited` fig. 2
    test("["+idc.getClass.getSimpleName+"] DC violation, cycle of negative edges.") {
      val A = idc.addVar()
      val B = idc.addVar()
      val C = idc.addVar()
      val D = idc.addVar()
      val E = idc.addVar()
      val U = idc.addVar()

      assert(idc.consistent)

      idc.enforceInterval(A, B, 5, 10)
      assert(idc.consistent)

      idc.enforceInterval(B, C, 5, 10)
      assert(idc.consistent)

      idc.enforceInterval(C, D, 5, 10)
      assert(idc.consistent)

      idc.enforceInterval(E, A, 5, 7)
      assert(idc.consistent)

      idc.addContingent(A, U, 5, 50)
      assert(idc.consistent)

      idc.addConstraint(U, D, -15)

      assert(!idc.consistent)
    }


  }



  for(idc <- getAllISTNU[String]) {

    // example from `Incremental Dynamic Controllability Revisited` fig. 2
    test("["+idc.getClass.getSimpleName+"] DC on the cooking dinner example. Incremental version.") {
      val WifeStore = idc.addVar()
      val StartDriving = idc.addVar()
      val WifeHome = idc.addVar()
      val StartCooking = idc.addVar()
      val DinnerReady = idc.addVar()

      idc.addContingent(WifeStore, StartDriving, 30, 60)
      assert(idc.consistent)

      idc.addContingent(StartDriving, WifeHome, 35, 40)
      assert(idc.consistent)

      idc.addContingent(StartCooking, DinnerReady, 25, 30)
      assert(idc.consistent)

      idc.addRequirement(WifeHome, DinnerReady, 5)
      assert(idc.consistent)

      idc.addRequirement(DinnerReady, WifeHome, 5)
      assert(idc.consistent)

//      idc.edg.exportToDot("before.dot", printer)


//      idc.edg.exportToDot("incremental.dot", printer)
//      val full = idc.cc()
//      full.checkConsistencyFromScratch()
//      full.edg.exportToDot("full.dot", printer)

      // make sure the cooking starts at the right time
//      val cc1 = idc.cc()
//      cc1.addRequirement(StartDriving, StartCooking, 9)
//      assert(!cc1.consistent)
//
//      val cc2 = idc.cc()
//      cc2.addRequirement(StartCooking, StartDriving, -11)
//      assert(!cc2.consistent)
      assert(idc.hasRequirement(StartDriving, StartCooking, 10))
      assert(idc.hasRequirement(StartCooking, StartDriving, -10))
    }
  }

  for(idc <- getAllISTNU[String]) {

    // example from `Incremental Dynamic Controllability Revisited` fig. 2
    test("["+idc.getClass.getSimpleName+"] DC on the cooking dinner example. Non-Incremental version.") {
      val WifeStore = idc.addVar()
      val StartDriving = idc.addVar()
      val WifeHome = idc.addVar()
      val StartCooking = idc.addVar()
      val DinnerReady = idc.addVar()

      idc.addContingent(WifeStore, StartDriving, 30, 60)

      idc.addContingent(StartDriving, WifeHome, 35, 40)

      idc.addRequirement(WifeHome, DinnerReady, 5)

      idc.addRequirement(DinnerReady, WifeHome, 5)

//      idc.edg.exportToDot("before.dot", printer)

      idc.addContingent(StartCooking, DinnerReady, 25, 30)
      assert(idc.consistent)

//      idc.edg.exportToDot("incremental.dot", printer)
//      val full = idc.cc()
//      full.checkConsistencyFromScratch()
//      full.edg.exportToDot("full.dot", printer)

      // make sure the cooking starts at the right time
      assert(idc.hasRequirement(StartDriving, StartCooking, 10))
      assert(idc.hasRequirement(StartCooking, StartDriving, -10))
    }
  }
}
