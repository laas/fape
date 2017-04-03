package fr.laas.fape.constraints.meta.domains

/**
  * A boolean domain where true == 1 and false == 0
  */
class BooleanDomain(vals: Array[Int]) extends Domain {

  def this(values: Set[Boolean]) = this(values.map(x => if(x) 1 else 0).toArray)

  override def size: Int = vals.length

  override def values: Iterable[Int] = vals

  override def remove(toRm: Int): Domain =
    if(vals.contains(toRm))
      new BooleanDomain((vals.toSet - toRm).toArray)
    else
      this

  override def contains(value: Int): Boolean = vals.contains(value)

  def contains(value: Boolean): Boolean =
    if(value)
      vals.contains(1)
    else
      vals.contains(0)

  override def add(value: Int): Domain = {
    assert(value == 0 || value == 1)
    if(vals.contains(value))
      this
    else
      new BooleanDomain((vals.toSet + value).toArray)
  }

  override def union(other: Domain): Domain = other match {
    case other: BooleanDomain =>
      new BooleanDomain(vals ++ other.values)
    case _ =>
      super.union(other)
  }
}
