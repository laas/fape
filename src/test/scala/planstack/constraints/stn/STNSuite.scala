package planstack.constraints.stn

import org.scalatest.FunSuite

class STNSuite extends FunSuite {

  test("start and end creation") {
    val stn = STN()
    assert(stn.size == 2)
  }

  test("Simple Consistency") {
    val s = STN()

    val u = s.addVar()
    assert(u == 2, "u should be the third variable (after start and end)")
    val v = s.addVar()
    assert(s.consistent)

    s.addConstraint(2, 0, 0)
    s.addConstraint(1, 2, 0)

    s.addConstraint(2, 3, 0)
    assert(s.consistent)

    s.addConstraint(3,2,0)
    assert(s.consistent)

    s.addConstraint(2,3,-1)
    s.checkConsistency()
    assert(! s.consistent)
  }

  test("STN cloning") {
    val stn = STN()

    val u = stn.addVar()
    val v = stn.addVar()
    stn.addConstraint(u, v, 10)

    val stnSize = stn.size

    val s2 = stn.cc().asInstanceOf[STNIncBellmanFord[Int]]
    s2.addConstraint(v, u, -20)

//    println("Clone : \n" + s2.distancesToString )
//    s2.writeToDotFile("/home/abitmonn/these/Documents/Experiments/tmp/g2.dot")

    assert(!s2.consistent, "s2 should be inconsistent (there is a negative cycle.")
    assert(stn.consistent, "The base stn shouldn't have moved")

    assert(stn.size == stnSize, "No new variable should have been added to the stn")
  }

  test("Earliest start") {
    val stn = STN()

    val u = stn.addVar()
    val v = stn.addVar()

    assert(stn.earliestStart(u) == 0)

    stn.enforceInterval(u, v, 10, 20)
    assert(stn.earliestStart(u) == 0)
    assert(stn.earliestStart(v) == 10)
    assert(stn.makespan == 10)
  }

  test("Edges sorted") {
    val stn = STN()

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
