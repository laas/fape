package fr.laas.fape.constraints.meta.domains

import fr.laas.fape.constraints.meta.util.Assertion._

trait Domain {

  /** Number of values in the domain */
  def size : Int

  def contains(value: Int) : Boolean

  /** All values in the domain */
  def values : Iterable[Int]

  /** Lowest value in the domain */
  def lb: Int

  /** Highest value in the domain */
  def ub: Int

  def intersection(other: Domain) : Domain = {
    val intersection = this.values.toSet.filter(other.contains)
    new EnumeratedDomain(intersection)
  }

  def &(other: Domain) : Domain = intersection(other)

  def union(other: Domain) : Domain = {
    val union = this.values.toSet ++ other.values
    new EnumeratedDomain(union)
  }

  def +(other: Domain) : Domain = union(other)

  def emptyIntersection(other: Domain) = (this & other).size == 0

  def containedBy(other: Domain) = (this & other).size == this.size

  def isSingleton : Boolean = size == 1

  def head : Int = { assert1(nonEmpty); values.head }

  def isEmpty : Boolean = size <= 0

  def nonEmpty = !isEmpty

  def remove(toRm: Domain) : Domain =
    new EnumeratedDomain(values.toSet.filterNot(toRm.contains))

  def -(toRm: Domain) = remove(toRm)

  def remove(toRm: Int) : Domain =
    new EnumeratedDomain(values.toSet - toRm)

  def -(toRm: Int) : Domain = remove(toRm)

  def add(value: Int) : Domain =
    new EnumeratedDomain(values.toSet + value)

  def +(value: Int) : Domain = add(value)

  override def equals(o: Any) : Boolean = o match {
    case o: Domain => o.values.toSet == values.toSet
    case _ => false
  }

  override def toString : String =
    if(isSingleton)
      values.head.toString
    else if(size <= 6)
      s"{${values.mkString(", ")}}"
    else
      s"{${values.take(3).mkString(", ")}, ..., ${values.takeRight(3)}}"
}

object Domain {

  /** Factory for Domain, using the most adapted representation for the given values */
  def apply(values: Set[Int]) : Domain =
    if(values.isEmpty) new EmptyDomain
    else if(values.size == 1) new SingletonDomain(values.head)
    else new EnumeratedDomain(values)

  def apply(values: Int*): Domain = Domain(values.toSet)
}