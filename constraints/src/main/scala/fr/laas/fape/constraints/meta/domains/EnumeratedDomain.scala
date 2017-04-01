package fr.laas.fape.constraints.meta.domains

import java.util

import fr.laas.fape.constraints.bindings.IBitSet

import scala.collection.JavaConverters._


object EnumeratedDomain {
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

class EnumeratedDomain(val vals: scala.collection.Set[Int]) extends Domain {

  def this(values: util.Collection[Integer]) = this(EnumeratedDomain.convert(values))
  def this(values: Iterable[Int]) = this(values.map(_.asInstanceOf[Integer]).asJavaCollection)
  def this(values: java.util.BitSet) = this(new IBitSet(values.toLongArray))

  def toBitSet : java.util.BitSet = vals match {
    case bs: IBitSet => java.util.BitSet.valueOf(bs.elems)
    case _ => throw new RuntimeException("Unsupported conversion from non-IBitSet collection.")
  }

  lazy private val _isEmpty = vals.isEmpty
  lazy private val _size = vals.size

  def values() : util.Set[Integer] = vals.asJava.asInstanceOf[java.util.Set[Integer]]

  def size : Integer = _size

  def head() : Integer = vals.head

  override def intersect(other: Domain) : EnumeratedDomain = {
    other match {
      case other: EnumeratedDomain =>
        val intersection = (vals, other.vals) match {
          case (v1: IBitSet, v2: IBitSet) => v1 & v2 // should be significantly faster as it is just and 'and' on two bitset
          case (v1, v2) => v1 & v2
        }
        new EnumeratedDomain(intersection)
    }
  }

  override def union(other: Domain) : EnumeratedDomain = {
    other match {
      case other: EnumeratedDomain =>
        val union = (vals, other.vals) match {
          case (v1: IBitSet, v2: IBitSet) => v1 | v2 // should be significantly faster as it is just and 'or' on two bitset
          case (v1, v2) => v1 | v2
        }
        new EnumeratedDomain(union)
    }
  }

  override def nonEmptyIntersection(other: Domain) : Boolean = {
    other match {
      case other: EnumeratedDomain =>
        (vals, other.vals) match {
          case (v1: IBitSet, v2: IBitSet) => v1.sharesOneElement(v2) // optimized method
          case (v1, v2) => (v1 & v2).nonEmpty
        }
    }
  }

  def contains(v: Int) : Boolean =
    vals.contains(v)

  override def isEmpty : Boolean = _isEmpty

  override def nonEmpty = !isEmpty

  override def remove(toRm: Domain) : EnumeratedDomain = {
    toRm match {
      case toRm: EnumeratedDomain =>
        new EnumeratedDomain (vals -- toRm.vals)
    }
  }

  def remove(toRm: Int) : EnumeratedDomain =
    new EnumeratedDomain(vals - toRm)

  def add(value: Int) : EnumeratedDomain = {
    //    if (vals.isInstanceOf[BitSet] && value > ValuesHolder.BITSET_MAX_SIZE)
    ////     it has grown too big, switch to too normal Set
    //      new ValuesHolder(Set[Int]() ++ vals + value)
    //    else
    new EnumeratedDomain(vals + value)
  }

  override def equals(o: Any) : Boolean = {
    o match {
      case o: EnumeratedDomain => o.vals == this.vals
    }
  }
}
