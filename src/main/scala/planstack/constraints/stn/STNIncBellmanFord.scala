package planstack.constraints.stn

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import StnPredef._

class STNIncBellmanFord(val q:mutable.Queue[Tuple2[Int,Int]],
                        val forwardDist:ArrayBuffer[Weight],
                        val backwardDist:ArrayBuffer[Weight],
                        sup_g: G,
                        sup_consistent:Boolean)
  extends STN(sup_g, sup_consistent) {

//  var updatedVars = List[Int]()

  def this() = this(new mutable.Queue[Tuple2[Int,Int]],
                    new ArrayBuffer[Weight](0),
                    new ArrayBuffer[Weight](0),
                    NewGraph(), true)


  /**
   * Data structures to keep track of the distances from/to 0 to/from all vertices
   * This is used as part of the Iterative Bellman-Ford algorithm.
   */
//  val forwardDist = new Array[planstack.constraints.stn.Weight](g.numVertices)
//  val backwardDist = new Array[planstack.constraints.stn.Weight](g.numVertices)

  override def addVarUnsafe() : Int = {
    val dist =
      if(g.numVertices == 0) new Weight(0)  // dist to origin = 0
      else Weight.InfWeight

    forwardDist.append(dist)
    backwardDist.append(dist)

    super.addVarUnsafe()
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
      consistent = incrementalBellmanFord()

    if(!consistent)
      println("Warning: STN is not consistent")
    consistent
  }

  def incrementalBellmanFord() : Boolean = {

    val (i,j) = q.dequeue()

    // Forward
    val w = getWeight(i, j)
    assert(!w.inf)

    val modified = new mutable.Queue[Int]()
    var consistent = true

    if(forwardDist(j) > forwardDist(i) + w) {
      forwardDist(j) = forwardDist(i) + w
      modified += j

      while(modified.nonEmpty && consistent) {
        val u = modified.dequeue()

        g.outEdges(u).foreach(e => {
          assert(e.u == u)
          if(forwardDist(e.v) > forwardDist(e.u) + e.l) {
            if(e.v == j) {
              consistent =  false // j updated twice, this is a negative cycle
            }
            forwardDist(e.v) = forwardDist(e.u) + e.l
            modified += e.v
          }
        })

      }
    }

    if(consistent && backwardDist(i) > backwardDist(j) + w) {
      backwardDist(i) = backwardDist(j) + w
      modified += i

      while(modified.nonEmpty && consistent) {
        // select next vertices to treat among the one that were modified
        val v = modified.dequeue()

        // update backward distance for all incoming edges
        for(e <- g.inEdges(v)) {
          assert(e.v == v)
          if(backwardDist(e.u) > backwardDist(e.v) + e.l) {
            if(e.u == i) {
              // i was updated twice. This means there is a negative cycle
              consistent = false
            }
            backwardDist(e.u) = backwardDist(e.v) + e.l
            modified += e.u
          }
        }
      }
    }

    /*
    for(e <- g.edges()) {
      println("%d -> %d : %d".format(e.u, e.v, e.l))
    }

    var str = "IBF : " + consistent.toString + " -> "
    for(i <- 0 to forwardDist.length-1) {
      str += "(%d %s)".format(i, forwardDist(i))
    }
    str += "\n           <- "
    for(i <- 0 to backwardDist.length-1) {
      str += "(%d %s)".format(i, backwardDist(i))
    }
    println(str)
    */

    return consistent
  }

  def cc() : STNIncBellmanFord = {
    new STNIncBellmanFord(q.clone(), forwardDist.clone(), backwardDist.clone(), g.cc().asInstanceOf[G], consistent)
  }

  def distancesToString = {
    var str = "IBF : " + consistent.toString + " start -> all"
    for(i <- 0 to forwardDist.length-1) {
      str += "(%d %s)".format(i, forwardDist(i))
    }
    str += "\n           start <- all"
    for(i <- 0 to backwardDist.length-1) {
      str += "(%d %s)".format(i, backwardDist(i))
    }
    str
  }

  def earliestStart(u: Int) =
    if(backwardDist(u).inf) Int.MinValue
    else - backwardDist(u).w

  def latestStart(u:Int) =
    if(forwardDist(u).inf) Int.MaxValue
    else forwardDist(u).w
}
