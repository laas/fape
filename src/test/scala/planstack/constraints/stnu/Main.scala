package planstack.constraints.stnu

import org.scalatest.FunSuite

class Main extends FunSuite {


  test("Main") {
    val fidc = new FastIDC

    fidc.addRequirement(2, 5, 10)
    fidc.addRequirement(5, 7, -10)

  }

}
