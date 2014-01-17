package planstack.constraints.stn

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
      for(e <- g.getEdges) {
        val new_dist = dist(e.orig) + e.w
        if(dist(e.dest) > new_dist)
          dist(e.dest) = new_dist
      }
    }

    var ret = true
    for(e <- g.getEdges) {
      if(dist(e.dest) > dist(e.orig) + e.w)
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
