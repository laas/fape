package fr.laas.fape.constraints.meta.domains

class EmptyDomain extends Domain {

  override def size: Int = 0

  override def values: Iterable[Int] = List()

  override def emptyIntersection(o: Domain): Boolean = true

  override def intersection(other: Domain): Domain = this

  override def remove(toRm: Domain): Domain = throw new UnsupportedOperationException

  override def remove(toRm: Int): Domain = throw new UnsupportedOperationException

  override def contains(value: Int): Boolean = false

  override def isEmpty: Boolean = true

  override def add(value: Int): Domain = new EnumeratedDomain(List(value))

  override def union(other: Domain): Domain = other

  /** Lowest value in the domain */
  override def lb: Int = ???

  /** Highest value in the domain */
  override def ub: Int = ???
}
