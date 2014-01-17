package planstack.constraints.stn

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class STNIBF(val q:mutable.Queue[Tuple2[Int,Int]],
             val forwardDist:ArrayBuffer[Weight],
             val backwardDist:ArrayBuffer[Weight],
             sup_g: Graph, sup_consistent:Boolean) extends STN(sup_g, sup_consistent) {

//  var updatedVars = List[Int]()

  def this() = this(new mutable.Queue[Tuple2[Int,Int]],
                    new ArrayBuffer[Weight](0),
                    new ArrayBuffer[Weight](0),
                    new AdjacencyList(), true)


  /**
   * Data structures to keep track of the distances from/to 0 to/from all vertices
   * This is used as part of the Iterative Bellman-Ford algorithm.
   */
//  val forwardDist = new Array[planstack.constraints.stn.Weight](g.numVertices)
//  val backwardDist = new Array[planstack.constraints.stn.Weight](g.numVertices)

  override def addVar() : Int = {
    val dist =
      if(g.numVertices == 1) new Weight(0)  // dist to origin = 0
      else Weight.InfWeight

    forwardDist.append(dist)
    backwardDist.append(dist)

    super.addVar()
  }

  override def addConstraintFast(v1:Int, v2:Int, w:Int) : Boolean = {
    val inserted = super.addConstraintFast(v1, v2, w)

    if(inserted) {
      q.enqueue((v1, v2))
    }

    inserted
  }

  def checkConsistency() : Boolean = {
    if(!q.isEmpty)
      consistent = iterativeBellmanFord()

    if(!consistent)
      println("Warning: STN is not consistent")
    consistent
  }

  def iterativeBellmanFord() : Boolean = {

    val (i,j) = q.dequeue()

    // Forward
    val w = g.getWeight(i, j)
    assert(!w.inf)

    val modified = new mutable.Queue[Int]()
    var consistent = true

    if(forwardDist(j) > forwardDist(i) + w) {
      forwardDist(j) = forwardDist(i) + w
      modified += j

      while(modified.nonEmpty && consistent) {
        val u = modified.dequeue()

        g.outEdges(u).foreach(e => {
          assert(e.orig == u)
          if(forwardDist(e.dest) > forwardDist(e.orig) + e.w) {
            if(e.dest == j) {
              consistent =  false // j updated twice, this is a negative cycle
            }
            forwardDist(e.dest) = forwardDist(e.orig) + e.w
            modified += e.dest
          }
        })

      }
    }

//        var str = "IBF : " + consistent.toString + " -> "
//        for(i <- 0 to forwardDist.length-1) {
//          str += "(%d %s)".format(i, forwardDist(i))
//        }
//        str += "\n           <- "
//        for(i <- 0 to backwardDist.length-1) {
//          str += "(%d %s)".format(i, backwardDist(i))
//        }
//        println(str)

    return consistent
  }

  override def clone() : STNIBF = {
    new STNIBF(q.clone(), forwardDist.clone(), backwardDist.clone(), g.clone(), consistent)
  }

}
