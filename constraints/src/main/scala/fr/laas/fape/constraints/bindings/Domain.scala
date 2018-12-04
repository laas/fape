package fr.laas.fape.constraints.bindings

import java.util

import scala.collection.JavaConversions
import scala.collection.JavaConverters._




object Domain {
  def convert(values: util.Collection[Integer]) : IBitSet = {
    var max = -10
    for(v <- values.asScala)
      max = if(v > max) v else max
    var set : IBitSet = new IBitSet()
    set ++= values.asInstanceOf[util.Collection[Int]].asScala
    set
  }
}

class Domain(val vals: IBitSet) {

  def this(values: util.Collection[Integer]) = this(Domain.convert(values))
  def this(values: Iterable[Int]) = this(values.map(_.asInstanceOf[Integer]).asJavaCollection)
  def this(values: java.util.BitSet) = this(new IBitSet(values.toLongArray))

  def toBitSet : java.util.BitSet = vals match {
    case bs: IBitSet => java.util.BitSet.valueOf(bs.elems)
    case _ => throw new RuntimeException("Unsupported conversion from non-IBitSet collection.")
  }
  private val _size = vals.size

  def values() : util.Set[Integer] = JavaConversions.setAsJavaSet(vals).asInstanceOf[java.util.Set[Integer]]

  def size() : Int = _size

  def head() : Int = vals.min

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

  def isEmpty : Boolean = _size == 0

  def nonEmpty = !isEmpty

  def remove(toRm: Domain) : Domain =
    new Domain(vals -- toRm.vals)

  def remove(toRm: Integer) : Domain =
    new Domain(vals - toRm)

  def add(value: Integer) : Domain =
      new Domain(vals + value)
}
