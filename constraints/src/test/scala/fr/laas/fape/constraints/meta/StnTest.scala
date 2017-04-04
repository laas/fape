package fr.laas.fape.constraints.meta

import org.scalatest.{BeforeAndAfter, FunSuite}

class StnTest extends FunSuite with BeforeAndAfter {

  implicit var csp: CSP = null

  before {
    csp = new CSP
  }


  test("Timepoint creation") {

    val tp1 = csp.varStore.getTimepoint("first")
    val tp2 = csp.varStore.getTimepoint("second")

    csp.post(tp1 < tp2)
    csp.post(csp.temporalHorizon <= 100)
    csp.propagate()

    println(csp.temporalOrigin.dom)
    println(tp1.dom)
    println(tp2.dom)
    println(csp.varStore.getDelayVariable(tp1, tp2).dom)
  }
}
