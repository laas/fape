package fr.laas.fape.constraints.meta.domains

trait Domain {

  def size : Int

  def contains(value: Int) : Boolean

  def intersect(other: Domain) : Domain

  def union(other: Domain) : Domain
  def +(other: Domain) : Domain = union(other)

  def emptyIntersection(other: Domain) = (this intersect other).size == 0

  def nonEmptyIntersection(o: Domain) : Boolean

  def isSingleton : Boolean = size == 1

  def isEmpty : Boolean

  def nonEmpty = !isEmpty

  def remove(toRm: Domain) : Domain
  def -(toRm: Domain) = remove(toRm)

  def remove(toRm: Int) : Domain
  def -(toRm: Int) : Domain = remove(toRm)

  def add(value: Int) : Domain
  def +(value: Int) : Domain = add(value)

  override def equals(o: Any) : Boolean
}
