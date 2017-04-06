package fr.laas.fape.constraints.meta.domains

class SingletonDomain(val value: Int) extends Domain {

  override def size: Int = 1

  override def values: Iterable[Int] = List(value)

  override def contains(value: Int): Boolean =
    if(value == this.value)
      true
    else
      false

  override def intersection(other: Domain) : Domain =
    if(other.contains(value))
      this
    else
      new EmptyDomain

  /** Lowest value in the domain */
  override def lb: Int = value

  /** Highest value in the domain */
  override def ub: Int = value
}
