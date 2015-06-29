package planstack.constraints.bindings

import java.util

import scala.collection.JavaConversions
import scala.collection.JavaConverters._
import scala.collection.immutable.BitSet

object ValuesHolder {
  val BITSET_MAX_SIZE = 100
  def convert(values: util.Collection[Integer]) : Set[Int] = {
    var max = -10
    for(v <- values.asScala)
      max = if(v > max) v else max
    var set : Set[Int] =
      if(max > BITSET_MAX_SIZE)
        Set[Int]()
      else
        BitSet()
    set ++= values.asInstanceOf[util.Collection[Int]].asScala
    set
  }
}

class ValuesHolder(val vals: scala.collection.Set[Int]) {

  def this(values: util.Collection[Integer]) = this(ValuesHolder.convert(values))
  def this(values: Iterable[Int]) = this(values.map(_.asInstanceOf[Integer]).asJavaCollection)

  lazy private val _isEmpty = vals.isEmpty
  lazy private val _size = vals.size

  def values() : util.Set[Integer] = JavaConversions.setAsJavaSet(vals).asInstanceOf[java.util.Set[Integer]]

  def size() : Integer = _size

  def head() : Integer = vals.head

  def intersect(other: ValuesHolder) : ValuesHolder =
    new ValuesHolder(vals intersect other.vals)

  def union(other: ValuesHolder) : ValuesHolder =
    new ValuesHolder(vals | other.vals)

  def contains(v: Integer) : Boolean =
    vals.contains(v)

  def isEmpty : Boolean = _isEmpty

  def nonEmpty = !isEmpty

  def remove(toRm: ValuesHolder) : ValuesHolder =
    new ValuesHolder(vals -- toRm.vals)

  def remove(toRm: Integer) : ValuesHolder =
    new ValuesHolder(vals - toRm)

  def add(value: Integer) : ValuesHolder = {
    if (vals.isInstanceOf[BitSet] && value > ValuesHolder.BITSET_MAX_SIZE)
    // it has grown too big, switch to too normal Set
      new ValuesHolder(Set[Int]() ++ vals + value)
    else
      new ValuesHolder(vals + value)
  }
}
