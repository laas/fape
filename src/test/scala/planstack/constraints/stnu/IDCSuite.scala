package planstack.constraints.stnu

import org.scalatest.FunSuite

class IDCSuite extends FunSuite {

  val idc = new FastIDC
  for(i <- 0 until 10) idc.addVar()

  test("Pseudo controllable") {
    val idc = new FastIDC
    for(i <- 0 until 10) idc.addVar()

    idc.enforceBefore(0, 1)
    idc.enforceBefore(0, 2)
    idc.enforceBefore(0, 3)

    idc.addContingent(1, 2, 15)
    idc.addContingent(2, 1, -10)

    idc.enforceBefore(2, 3)
    idc.enforceInterval(0, 3, 16, 16)

    assert(idc.consistent)
  }

  test("Not pseudo controllable") {
    val idc = new FastIDC
    for(i <- 0 until 10) idc.addVar()

    idc.enforceBefore(0, 1)
    idc.enforceBefore(0, 2)
    idc.enforceBefore(0, 3)

    idc.addContingent(1, 2, 15)
    idc.addContingent(2, 1, -10)

    idc.enforceBefore(2, 3)
    idc.enforceInterval(0, 3, 16, 12)

    assert(!idc.consistent)
  }
}
