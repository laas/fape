package fr.laas.fape.constraints.stnu.structurals

import java.util

import fr.laas.fape.constraints.stnu.InconsistentTemporalNetwork


import scala.collection.mutable

object DistanceMatrix {
  /** number of spot to create when needing to grow the network */
  val growthIncrement = 5

  /** Infinity value that does not overflow when added to itself */
  val INF :Int = Integer.MAX_VALUE /2 -1

  /** Addition that will never overflow given that both parameters are in [-INF,INF] */
  final def plus(a:Int, b: Int) = {
    assert(a <= INF && a > -INF)
    assert(b <= INF && b > -INF)
    if(a + b >= INF)
      INF
    else if(a == INF || b == INF)
      INF
    else
      a + b
  }
}

trait DistanceMatrixListener {
  def distanceUpdated(a: Int, b: Int)
}

import DistanceMatrix._

class DistanceMatrix(
                      private var dists: Array[Array[Int]],
                      private val emptySpots: mutable.Set[Int]
                    ) {

  def this() = this(new Array[Array[Int]](0), mutable.Set())

  override def clone() : DistanceMatrix = {
    val newDists = new Array[Array[Int]](dists.length)
    for(i <- dists.indices if dists(i) != null)
      newDists(i) = util.Arrays.copyOf(dists(i), dists(i).length)
    val newEmptySpots = emptySpots.clone()
    new DistanceMatrix(newDists, newEmptySpots)
  }

  private val listeners = mutable.ArrayBuffer[DistanceMatrixListener]()
  def addListener(listener: DistanceMatrixListener): Unit = {
    listeners += listener
  }

  private final def isActive(tp: Int) = {
    assert(tp < dists.size)
    !emptySpots.contains(tp)
  }

  /**
    * Initialize a new node, possibly growing the matrix.
    * Distances to self is set to 0 and distances to other nodes is set to INF
    * @return The id of the new node
    */
  def createNewNode(): Int = {
    if(emptySpots.isEmpty) {
      // grow matrix
      val prevLength = dists.length
      val newLength = prevLength + growthIncrement
      val newDists = util.Arrays.copyOf(dists, newLength)
      for(i <- 0 until prevLength) {
        newDists(i) = util.Arrays.copyOf(dists(i), newLength)
        util.Arrays.fill(newDists(i), prevLength, newLength, INF)
      }
      for(i <- prevLength until newLength) {
        newDists(i) = new Array[Int](newLength)
        util.Arrays.fill(newDists(i), INF)
        emptySpots += i
      }
      dists = newDists
    }
    val newNode = emptySpots.head
    emptySpots -= newNode
    dists(newNode)(newNode) = 0
    newNode
  }

  /**
    * Removes a node from the network. Note that all constraints previously inferred will stay in the matrix
    */
  private def eraseNode(n: Int): Unit = {
    util.Arrays.fill(dists(n), INF)
    for(i <- dists.indices)
      dists(i)(n) = INF
    emptySpots += n
  }

  /**
    * Add a new distance in the matrix and propagate to get the minimal network
    * using an incremental Floyd Warshall algorithm.
    * All listeners are called backed for each updated edge (given that both the source and the
    * target are still in the matrix at the time of the callback).
    */
  def enforceDist(a: Int, b: Int, d: Int): Unit = {
    if(plus(d, dists(b)(a)) < 0)
      throw new InconsistentTemporalNetwork
    if(d >= dists(a)(b))
      return // constraint is dominated

    dists(a)(b) = d
    val updatedEdges = mutable.ArrayBuffer[(Int,Int)]()
    updatedEdges += ((a,b))
    val nodes = dists.indices.filterNot(emptySpots.contains)

    val I = mutable.ArrayBuffer[Int]()
    val J = mutable.ArrayBuffer[Int]()
    for(k <- nodes if k != a && k!= b) {
      if(dists(k)(b) > plus(dists(k)(a), d)) {
        dists(k)(b) = plus(dists(k)(a), d)
        updatedEdges += ((k,b))
        I += k
      }
      if(dists(a)(k) > plus(d, dists(b)(k))) {
        dists(a)(k) = plus(d, dists(b)(k))
        updatedEdges += ((a,k))
        J += k
      }
    }
    for(i <- I ; j <- J if i != j) {
      if(dists(i)(j) > plus(dists(i)(a), dists(a)(j))) {
        dists(i)(j) = plus(dists(i)(a), dists(a)(j))
        updatedEdges += ((i,j))
      }
    }
    for((u,v) <- updatedEdges)
      updated(u, v)
  }

  /**
    * Removes a timepoint from the matrix given that it is rigidly constrained to another one.
    * @param anchoredTimepoint Timepoint to remove
    * @param anchor Timepoint that serve as an anchor to the remove one.
    */
  def compileAwayRigid(anchoredTimepoint: Int, anchor: Int): Unit = {
    assert(isActive(anchoredTimepoint))
    assert(isActive(anchor))
    assert(anchoredTimepoint != anchor)
    assert(dists(anchor)(anchoredTimepoint) == -dists(anchoredTimepoint)(anchor), "Trying to compile a non rigid relation")
    if(StnWithStructurals.debugging) { // check that all distances are consistent
      for (i <- dists.indices if isActive(i)) {
        assert(dists(i)(anchor) == plus(dists(i)(anchoredTimepoint), dists(anchoredTimepoint)(anchor)))
        assert(dists(anchor)(i) == plus(dists(anchor)(anchoredTimepoint), dists(anchoredTimepoint)(i)))
      }
    }
    eraseNode(anchoredTimepoint)
  }

  def getDistance(a: Int, b: Int): Int = {
    assert(isActive(a) && isActive(b))
    dists(a)(b)
  }

  private final def updated(a: Int, b: Int): Unit = {
    if(isActive(a) && isActive(b)) {
      assert(dists(a)(b) < INF)
      for (list <- listeners)
        list.distanceUpdated(a, b)
    }
  }
}
