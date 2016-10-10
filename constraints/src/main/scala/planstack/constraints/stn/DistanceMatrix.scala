package planstack.constraints.stn

import java.util

import planstack.constraints.stnu.InconsistentTemporalNetwork

import scala.collection.mutable

object DistanceMatrix {
  val growthIncrement = 5
  val INF :Int = Integer.MAX_VALUE /2 -1

  /** addition that will never overflow given that both parameters are in [-INF,INF] */
  final def plus(a:Int, b: Int) = {
    if(a + b > INF)
      INF
    else if(a +b < -INF)
      -INF
    else
      a + b
  }
}

import DistanceMatrix._

class DistanceMatrix(
                      var dists: Array[Array[Int]],
                      val emptySpots: mutable.Set[Int],
                      val defaultValue: Int = -INF
                    ) {

  def getNewNode(): Int = {
    if(emptySpots.isEmpty) {
      // grow matrix
      val prevLength = dists.length
      val newLength = prevLength + growthIncrement
      val newDists = util.Arrays.copyOf(dists, newLength)
      for(i <- 0 to prevLength) {
        newDists(i) = util.Arrays.copyOf(dists(i), newLength)
        util.Arrays.fill(newDists(i), prevLength, newLength, defaultValue)
      }
      for(i <- prevLength to newLength) {
        newDists(i) = new Array[Int](newLength)
        util.Arrays.fill(newDists(i), defaultValue)
        emptySpots += i
      }
      dists = newDists
    }
    val newNode = emptySpots.head
    emptySpots -= newNode
    newNode
  }

  /**
    * Removes a node from the network. Note that all constraints previously inferred will stay in the matrix
    */
  def eraseNode(n: Int): Unit = {
    util.Arrays.fill(dists(n), defaultValue)
    for(i <- dists.indices)
      dists(i)(n) = defaultValue
    emptySpots += n
  }

  def enforceDist(a: Int, b: Int, d: Int): Unit = {
    if(plus(d, dists(b)(a)) < 0)
      throw new InconsistentTemporalNetwork
    if(d >= dists(a)(b))
      return // constraint is dominated

    dists(a)(b) = d

    val I = mutable.ArrayBuffer[Int]()
    val J = mutable.ArrayBuffer[Int]()
    for(k <- dists.indices if !emptySpots.contains(k) && k != a && k!= b) {
      if(dists(k)(b) > plus(dists(k)(a), d)) {
        dists(k)(b) = plus(dists(k)(a), d)
        updated(k,b)
        I += k
      }
      if(dists(a)(k) < plus(d, dists(b)(k))) {
        dists(a)(k) = plus(d, dists(b)(k))
        updated(a,k)
        J += k
      }
    }
    for(i <- I ; j <- J if i != j && !emptySpots.contains(i) && !emptySpots.contains(j)) {
      if(dists(i)(j) > plus(dists(i)(a), dists(a)(j))) {
        dists(i)(j) = plus(dists(i)(a), dists(a)(j))
        updated(i,j)
      }

    }
  }

  private final def updated(a: Int, b: Int)
}
