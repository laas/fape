package fr.laas.fape.constraints.meta.domains

import java.util

import fr.laas.fape.constraints.meta.types.Type
import fr.laas.fape.constraints.meta.util.Assertion._

class ExtensionDomain(val tupleSize: Int) {

  protected var numTuples = 0
  protected var tuples: Array[Int] = Array(tupleSize * 10)
  val individualDomains: Array[Domain] = Array.fill(tupleSize)(new EmptyDomain)

  protected def capacity = tuples.length / tupleSize

  def addTuple(tuple: Array[Int]) {
    assert1(tuple.length == tupleSize)
    if(capacity == numTuples)
      tuples = util.Arrays.copyOf(tuples, tuples.length*2)
    Array.copy(tuple, 0, tuples, numTuples * tupleSize, tupleSize)
    numTuples += 1
    for(i <- 0 until tupleSize)
      if(!individualDomains(i).contains(tuple(i)))
        individualDomains(i) = individualDomains(i) + tuple(i)
  }
}

class TypedExtensionDomain(val types: Seq[Type[Any]])
