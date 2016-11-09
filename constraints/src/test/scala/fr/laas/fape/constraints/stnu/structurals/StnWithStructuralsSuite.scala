package fr.laas.fape.constraints.stnu.structurals

import org.scalatest.FunSuite

class StnWithStructuralsSuite extends FunSuite {

  test("basic") {
    val pb =
      """
        |(define
        |  (:timepoints (dispatchable 1) (structural 2))
        |  (:constraints (min-delay 1 2 5) (min-delay 2 1 -5))
        |)
      """.stripMargin
    val stn = StnWithStructurals.buildFromString(pb)
    assert(stn.isConsistent())
  }
//
//  test("load from file") {
//    val source = scala.io.Source.fromFile("/tmp/stn.txt")
//    val lines = try source.mkString finally source.close()
//    StnWithStructurals.debugging = true
//    val stn = StnWithStructurals.buildFromString(lines)
//    assert(stn.isConsistent())
//  }

}
