package planstack.constraints.stn

import org.scalatest.Suite

class ConstraintRemovalSuite extends Suite {

  val stn = new STNIncBellmanFord()

  def testRemoval {
    var clone = stn.cc()

    def checkCloneEqSTN {
      assert((0 to stn.size-1).forall(i => stn.forwardDist(i) == clone.forwardDist(i)))
      assert((0 to stn.size-1).forall(i => stn.backwardDist(i) == clone.backwardDist(i)))
    }
    val u = stn.addVar()
    val v = stn.addVar()
    val w = stn.addVar()



    clone = stn.cc()
    clone.checkConsistencyFromScratch()
    checkCloneEqSTN

    stn.addConstraint(u, v, 5)
    stn.addConstraint(u, v, -5)

    clone = stn.cc()
    clone.checkConsistencyFromScratch()
    checkCloneEqSTN


    clone = stn.cc()
    stn.addConstraint(u,w, 10)
    stn.removeConstraint(u, w)
    checkCloneEqSTN


  }

}
