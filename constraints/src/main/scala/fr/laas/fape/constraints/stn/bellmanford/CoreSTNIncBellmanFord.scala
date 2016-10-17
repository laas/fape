package fr.laas.fape.constraints.stn.bellmanford

import fr.laas.fape.constraints.stn.{GenCoreSTN, StnPredef, Weight}
import StnPredef._
import planstack.graph.core.{LabeledDigraph, LabeledEdge}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class CoreSTNIncBellmanFord[ID](
                             val q:mutable.Queue[Tuple2[Int,Int]],
                             val forwardDist:ArrayBuffer[Weight],
                             val backwardDist:ArrayBuffer[Weight],
                             sup_g: LabeledDigraph[Int,Int],
                             notIntegrated : List[LabeledEdge[Int,Int]],
                             emptySpots : Set[Int],
                             sup_consistent:Boolean)
  extends GenCoreSTN[ID](sup_g, notIntegrated, sup_consistent, emptySpots) {

  def this() = this(
    new mutable.Queue[Tuple2[Int,Int]],
    new ArrayBuffer[Weight](0),
    new ArrayBuffer[Weight](0),
    NewGraph(),
    List(),
    Set(),
    true)


  override def addVarUnsafe() : Int = {
    val dist =
      if(g.numVertices == 0) new Weight(0)  // dist to origin = 0
      else Weight.InfWeight

    forwardDist.append(dist)
    backwardDist.append(dist)

    super.addVarUnsafe()
  }

  override def addConstraintFast(v1:Int, v2:Int, w:Int, optID:Option[ID]) : Boolean = {
    val inserted = super.addConstraintFast(v1, v2, w, optID)

    if(inserted) {
      q.enqueue((v1, v2))
    }

    inserted
  }

  def checkConsistency() : Boolean = {
    if(!q.isEmpty)
      consistent = incrementalBellmanFord()
    assert(q.isEmpty)

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

    //backward
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

    return consistent
  }

  def checkConsistencyFromScratch() : Boolean = {
    consistent = recomputeAllDistances()
    consistent
  }



  /**
   * A basic implementation of Bellman-Ford that computes all distances from scratch
    *
    * @return True if the STN is consistent (no negative cycles)
   */
  def recomputeAllDistances() : Boolean = {
    // set all distances to inf except for the origin
    for(v <- g.vertices) {
      val initialDist =
        if(v == start) new Weight(0)
        else Weight.InfWeight
      forwardDist(v) = initialDist
      backwardDist(v) = initialDist
    }

    // O(n*e): recomputes check all edges n times. (necessary because the graph is cyclic with negative values
    var i = 0
    var updated = true
    while(i < g.numVertices && updated) {
      updated = false
      i += 1
      for(e <- g.edges()) {
        if(forwardDist(e.u) + e.l < forwardDist(e.v)) {
          forwardDist(e.v) = forwardDist(e.u) + e.l
          updated = true
        }
        if(backwardDist(e.v) + e.l < backwardDist(e.u)) {
          backwardDist(e.u) = backwardDist(e.v) + e.l
          updated = true
        }
      }
    }

    // STN is consistent iff all distances are not updatable
    val consistent = g.edges().forall(e => {
      forwardDist(e.v) <= forwardDist(e.u) + e.l && backwardDist(e.u) <= backwardDist(e.v) + e.l
    })

    consistent
  }

  def cc(): CoreSTNIncBellmanFord[ID] = {
    new CoreSTNIncBellmanFord(q.clone(), forwardDist.clone(), backwardDist.clone(), g.cc, notIntegrated, emptySpots, consistent)
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
