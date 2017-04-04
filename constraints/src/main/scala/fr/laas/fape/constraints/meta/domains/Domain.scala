package fr.laas.fape.constraints.meta.domains

trait Domain {

  def size : Int

  def contains(value: Int) : Boolean

  def values : Iterable[Int]

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

  def isSingleton : Boolean = size == 1

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
    if(size <= 6)
      s"{${values.mkString(", ")}}"
    else
      s"{${values.take(3).mkString(", ")}, ..., ${values.takeRight(3)}}"
}
