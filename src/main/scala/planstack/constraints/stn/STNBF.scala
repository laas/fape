package planstack.constraints.stn


/** TODO This class is now broken.
  * It would be interesting to fix for testing purposes.
  *
class STNBF extends STN {

  def checkConsistency(): Boolean = {
    consistent = bellmanFord()

    if(!consistent)
      println("Warning: STN is not consistent")

    consistent
  }

  def bellmanFord() : Boolean = {
    val origin = 0
    val dist = Array.fill[Weight](g.numVertices)(Weight.InfWeight)
    dist(origin) = new Weight(0)

    for(i <- 1 to g.numVertices) {
      for(e <- g.edges()) {
        val new_dist = dist(e.u) + e.l
        if(dist(e.v) > new_dist)
          dist(e.v) = new_dist
      }
    }

    var ret = true
    for(e <- g.edges()) {
      if(dist(e.v) > dist(e.u) + e.l)
        ret = false
    }

    //    var str = ret.toString + " "
    //    for(i <- 0 to dist.length-1) {
    //      str += "(%d %s)".format(i, dist(i))
    //    }
    //    println(str)
    ret
  }
}
*/