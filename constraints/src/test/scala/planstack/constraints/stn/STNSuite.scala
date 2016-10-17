package planstack.constraints.stn

import org.scalatest.FunSuite
import planstack.constraints.stn.bellmanford.CoreSTNIncBellmanFord
import planstack.constraints.stnu.FullSTN

class STNSuite extends FunSuite {

  for(stn <- Predef.getAllISTN[String]) {
    test("[" + stn.getClass.getSimpleName + "] start and end creation") {
      assert(stn.size == 2)
    }
  }

  for(stn <- List(new CoreSTNIncBellmanFord[String](), new FullSTN[String](10))) {

    test("[" + stn.getClass.getSimpleName + "] Simple Consistency") {
      val u = stn.addVar()
      assert(u == 2, "u should be the third variable (after start and end)")
      val v = stn.addVar()
      assert(stn.consistent)

      stn.addConstraint(2, 0, 0)
      stn.addConstraint(1, 2, 0)

      stn.addConstraint(2, 3, 0)
      assert(stn.consistent)

      stn.addConstraint(3, 2, 0)
      assert(stn.consistent)

      stn.addConstraint(2, 3, -1)
      stn.checkConsistency()
      assert(!stn.consistent)
    }
  }

  for(stn <- Predef.getAllISTN[String]) {

    test("[" + stn.getClass.getSimpleName + "] STN cloning") {
      val u = stn.addVar()
      val v = stn.addVar()
      stn.addConstraint(u, v, 10)

      val stnSize = stn.size

      val s2 = stn.cc()
      s2.addConstraint(v, u, -20)

      //    println("Clone : \n" + s2.distancesToString )
      //    s2.writeToDotFile("/home/abitmonn/these/Documents/Experiments/tmp/g2.dot")

      assert(!s2.consistent, "s2 should be inconsistent (there is a negative cycle.")
      assert(stn.consistent, "The base stn shouldn't have moved")

      assert(stn.size == stnSize, "No new variable should have been added to the stn")
    }
  }

  for(stn <- Predef.getAllISTN[String]) {

    test("[" + stn.getClass.getSimpleName + "] Earliest start") {
      val u = stn.addVar()
      val v = stn.addVar()

      assert(stn.consistent)
      assert(stn.earliestStart(u) == 0)

      stn.enforceInterval(u, v, 10, 20)
      assert(stn.consistent)
      assert(stn.earliestStart(u) == 0)
      assert(stn.earliestStart(v) == 10)
      assert(stn.makespan == 10)
    }
  }

  test("Edges sorted for STNIncBellmanFord") {
    val stn = new CoreSTNIncBellmanFord[String]()

    val u = stn.addVar()
    val v = stn.addVar()
    stn.addConstraint(u, v, 10)
    stn.addConstraint(u, v, 100)
    stn.addConstraint(u, v, 5)

    stn.g.edges(u, v).foldLeft(Int.MinValue)((max, e) => {
      assert(max <= e.l, "the weight on the edges should be growing (since a constraint that does not reduces the value is useless and shouldn't be inserted to the graph")
      e.l
    })
  }

}
