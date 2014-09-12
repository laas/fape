package planstack.constraints.stnu

import org.scalatest.FunSuite

class UncontrollableAction extends FunSuite {

  test("Uncontrollable action") {
    val idc = new FastIDC

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
