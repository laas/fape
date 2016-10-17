package fr.laas.fape.constraints.bindings

import java.util

import scala.collection.JavaConversions
import scala.collection.JavaConverters._




object Domain {
//  val BITSET_MAX_SIZE =   // TODO: could be interesting to switch to TreeSet when reaching big sizes and low densities
  def convert(values: util.Collection[Integer]) : Set[Int] = {
    var max = -10
    for(v <- values.asScala)
      max = if(v > max) v else max
    var set : Set[Int] =
//      if(max > BITSET_MAX_SIZE)
//        Set[Int]()
//      else
        new IBitSet()
    set ++= values.asInstanceOf[util.Collection[Int]].asScala
    set
  }
}

class Domain(val vals: scala.collection.Set[Int]) {

  def this(values: util.Collection[Integer]) = this(Domain.convert(values))
  def this(values: Iterable[Int]) = this(values.map(_.asInstanceOf[Integer]).asJavaCollection)
  def this(values: java.util.BitSet) = this(new IBitSet(values.toLongArray))

  def toBitSet : java.util.BitSet = vals match {
    case bs: IBitSet => java.util.BitSet.valueOf(bs.elems)
    case _ => throw new RuntimeException("Unsupported conversion from non-IBitSet collection.")
  }

  lazy private val _isEmpty = vals.isEmpty
  lazy private val _size = vals.size

  def values() : util.Set[Integer] = JavaConversions.setAsJavaSet(vals).asInstanceOf[java.util.Set[Integer]]

  def size() : Integer = _size

  def head() : Integer = vals.head

  def intersect(other: Domain) : Domain = {
    val intersection = (vals, other.vals) match {
      case (v1: IBitSet, v2:IBitSet) => v1 & v2 // should be significantly faster as it is just and 'and' on two bitset
      case (v1, v2) => v1 & v2
    }
    new Domain(intersection)
  }

  def union(other: Domain) : Domain = {
    val union = (vals, other.vals) match {
      case (v1: IBitSet, v2: IBitSet) => v1 | v2 // should be significantly faster as it is just and 'or' on two bitset
      case (v1, v2) => v1 | v2
    }
    new Domain(union)
  }

  def hasOneCommonElement(o: Domain) : Boolean = {
    (vals, o.vals) match {
      case (v1: IBitSet, v2:IBitSet) => v1.sharesOneElement(v2) // optimized method
      case (v1, v2) => (v1 & v2).nonEmpty
    }
  }

  def contains(v: Integer) : Boolean =
    vals.contains(v)

  def isEmpty : Boolean = _isEmpty

  def nonEmpty = !isEmpty

  def remove(toRm: Domain) : Domain =
    new Domain(vals -- toRm.vals)

  def remove(toRm: Integer) : Domain =
    new Domain(vals - toRm)

  def add(value: Integer) : Domain = {
//    if (vals.isInstanceOf[BitSet] && value > ValuesHolder.BITSET_MAX_SIZE)
////     it has grown too big, switch to too normal Set
//      new ValuesHolder(Set[Int]() ++ vals + value)
//    else
      new Domain(vals + value)
  }
}
