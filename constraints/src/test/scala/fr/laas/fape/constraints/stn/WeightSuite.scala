package fr.laas.fape.constraints.stn

import org.scalatest.FunSuite

class WeightSuite extends FunSuite {

  val inf = new Weight()
  val big = new Weight(100)
  val small = new Weight(1)
  val neg = new Weight(-20)

  test("Comparisons") {
    assert(inf > big)
    assert(big < inf)
    assert(!(inf < big))
    assert(big > small)
    assert(!(small > big))
    assert(small < big)
    assert(!(big < small))

    assert(inf > neg)
    assert(!(neg > inf))
    assert(neg < big)
    assert(!(neg > big))
  }

  test("Additions") {
    var w = big + small
    assert(w > big)
    assert(w < inf)
    assert(w.w == big.w + small.w)

    w = big + neg
    assert(w < big)
    assert(w < inf)
    assert(w.w == big.w + neg.w)

    w = neg + inf
    assert(w > big)
    assert(w.inf)
  }
}
