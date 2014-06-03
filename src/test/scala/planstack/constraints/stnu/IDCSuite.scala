package planstack.constraints.stnu

import org.scalatest.FunSuite

class IDCSuite extends FunSuite {

  val idc = new FastIDC
  for(i <- 0 until 10) idc.addVar()

  test("Pseudo controllable") {
    val idc = new FastIDC
    val A = idc.addVar()
    val B = idc.addVar()
    val C = idc.addVar()
    val D = idc.addVar()

    idc.enforceBefore(A, B)
    idc.enforceBefore(A, C)
    idc.enforceBefore(A, D)

    idc.addContingent(B, C, 10, 15)

    idc.enforceBefore(C, D)
    idc.enforceInterval(A, D, 16, 16)

    assert(idc.consistent)
  }

  test("Not pseudo controllable") {
    val idc = new FastIDC
    for(i <- 0 until 10) idc.addVar()

    val A = idc.addVar()
    val B = idc.addVar()
    val C = idc.addVar()
    val D = idc.addVar()

    idc.enforceBefore(A, B)
    idc.enforceBefore(A, C)
    idc.enforceBefore(A, D)

    idc.addContingent(B, C, 10, 15)

    idc.enforceBefore(C, D)

    assert(idc.consistent)
    idc.enforceInterval(A, D, 12, 12)

    assert(!idc.consistent)
  }
}
