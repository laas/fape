package fr.laas.fape.constraints.meta.domains

import java.util

import fr.laas.fape.constraints.meta.types.statics.Type
import fr.laas.fape.constraints.meta.util.Assertion._

import scala.collection.mutable

/** Stores and indexes tuples of N values.
  *
  * It is to be used together with an ExtensionConstraint on N variables.
  * Each tuple identify a possible assignment of the N variables. */
class ExtensionDomain(val numVariables: Int) {

  private var locked = false

  protected var numTuples = 0
  protected var tuples: Array[Int] = new Array(numVariables * 10)
  val individualDomains: Array[Domain] = Array.fill(numVariables)(new EmptyDomain)

  /** Matches the possible values of a variable to the bindings they appear in. For instance:
    * [ { a1 -> {0,1}, a2 -> {2,3} },
    *   { b1 -> {0,2}, b2 -> {1,3} } ]
    *  means:
    *   - the value a1 of the first variable appears in the 0th and 1st bindings
    *   - the value a2 of the first variable appears in the 2nd and 3rd bindings
    *   - the value b1 of the second variable appears in the 0th and 2nd bindings
    *   - the value b2 of the second variable appears in the 1st and 3rd bindings
    *
    * Corresponding to the tuples: [(a1, b1), (a1, b2), (a2, b1), (a2, b2)]
    */
  private val tupleIndex: Array[mutable.Map[Int, util.BitSet]] = new Array(numVariables)
  for(i <- 0 until numVariables)
    tupleIndex(i) = mutable.Map()

  protected def capacity = tuples.length / numVariables

  private def value_at(tupleIndex: Int, variableIndex: Int) : Int =
    tuples(tupleIndex*numVariables + variableIndex)

  def hasTuple(values: Seq[Int]) : Boolean = {
    for(tuple <- 0 until numTuples) {
      if((0 until numVariables).forall(variable => values(variable) == value_at(tuple, variable)))
        return true
    }
    false
  }

  def addTuple(tuple: Seq[Int]) {
    assert1(!locked, "Trying to add a tuple to and extension domain already locked (i.e. that has been used for propagation)")
    assert1(tuple.length == numVariables)
    if(capacity == numTuples)
      tuples = util.Arrays.copyOf(tuples, tuples.length*2)
    Array.copy(tuple.toArray, 0, tuples, numTuples * numVariables, numVariables)
    numTuples += 1
    for(i <- 0 until numVariables) {
      if(!individualDomains(i).contains(tuple(i)))
        individualDomains(i) = individualDomains(i) + tuple(i)

      // record that the value tuple(i) of the i-th variable appears in the last tuple
      tupleIndex(i).getOrElseUpdate(tuple(i), new util.BitSet()).set(numTuples-1)
    }
  }

  /** Returns subsets of the given domains so that each value corresponds to a feasible tuple. */
  def restrictedDomains(originalDomains: Seq[Domain]): Seq[Domain] = {
    locked = true // used for propagation, make sure no other tuples will be added
    assert1(originalDomains.length == numVariables)
    val unaryReducedDomains = individualDomains.zip(originalDomains).map{ case (x, y) => x.intersection(y)}

    // will contain a booleans stating if the ith binding is valid according to the given domains
    val validTuples = new util.BitSet(numTuples)
    // at first they are all interesting
    validTuples.set(0, numTuples)

    for(variable <- 0 until numVariables) {
      val local = new util.BitSet(numTuples)
      for(value <- unaryReducedDomains(variable).values) {
        // get all tuples having the (variable, value) pair
        local.or(tupleIndex(variable)(value))
      }
      // keep only in valid tuples those valid for the current variable.
      validTuples.and(local)
    }

    val domainsFromValidTuples = new Array[util.BitSet](numVariables)
    for(i <- 0 until numVariables)
      domainsFromValidTuples(i) = new util.BitSet()

    // reconstruct domains from valid tuples
    var currentTupleIndex = validTuples.nextSetBit(0)
    while(currentTupleIndex >= 0) {
      for(variable <- 0 until numVariables)
        domainsFromValidTuples(variable).set(value_at(currentTupleIndex, variable))
      currentTupleIndex = validTuples.nextSetBit(currentTupleIndex+1)
    }

    val finalDomain = domainsFromValidTuples.map(x => new EnumeratedDomain(x))
    assert3((0 until numVariables).forall(i => finalDomain(i).size <= originalDomains(i).size))
    finalDomain
  }
}

class TypedExtensionDomain(val types: Seq[Type[Any]])
