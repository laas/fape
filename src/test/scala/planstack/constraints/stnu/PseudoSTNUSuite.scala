package planstack.constraints.stnu

import org.scalatest.FunSuite
import planstack.constraints.stn.Predef._

class PseudoSTNUSuite extends FunSuite {

  for(stnu <- getAllSTNUManager[String,String]) {
    test("["+stnu.getClass.getSimpleName+"] Pseudo consistency") {

      assert(stnu.checksPseudoConsistency)

      stnu.recordTimePoint("a")
      stnu.recordTimePoint("b")
      stnu.enforceContingent("a", "b", 10, 15)
      assert(stnu.isConsistent)

      stnu.enforceMinDelay("a", "b", 11)
      assert(!stnu.isConsistent)

    }
  }

}
