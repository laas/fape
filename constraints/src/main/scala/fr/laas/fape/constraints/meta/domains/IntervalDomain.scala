package fr.laas.fape.constraints.meta.domains

class IntervalDomain(val lb: Int, val ub: Int) extends Domain {
  require(lb <= ub)

  override def size: Int = math.max(0, ub - lb + 1)

  override def values: Iterable[Int] = lb to ub

  override def intersection(other: Domain): Domain = other match {
    case other: IntervalDomain =>
      new IntervalDomain(math.max(lb, other.lb), math.min(ub, other.lb))
    case other: EnumeratedDomain =>
      new EnumeratedDomain(other.values.filter(v => lb <= v && v <= ub))
    case _ =>
      super.intersection(other)
  }

  private def strictlyContains(other: IntervalDomain) = lb < other.lb && other.ub < ub
  private def contains(other: IntervalDomain) = lb <= other.lb && other.ub <= ub

  override def remove(other: Domain): Domain = other match {
    case other: IntervalDomain if !strictlyContains(other) =>
      if(emptyIntersection(other))
        this
      else if(other.contains(this))
        new EmptyDomain
      else if(other.lb < ub)
        new IntervalDomain(lb, other.lb-1)
      else {
        assert(other.ub > lb)
        new IntervalDomain(other.ub+1, ub)
      }

    case _ =>
      super.remove(other)
  }

  override def remove(toRm: Int): Domain = {
    if(toRm < lb || toRm > ub) this
    else if(this.isSingleton) new EmptyDomain
    if(toRm == lb) new IntervalDomain(lb+1, ub)
    else if(toRm == ub) new IntervalDomain(lb, ub-1)
    else super.remove(toRm) // value in the interval, resort to default method
  }

  override def contains(value: Int): Boolean = lb <= value && value <= ub

  override def add(value: Int): Domain = {
    if(value == lb-1)
      new IntervalDomain(lb-1, ub)
    else if(value == ub+1)
      new IntervalDomain(lb, ub+1)
    else if(lb <= value && value <= ub)
      this
    else
      super.add(value)
  }

  override def union(other: Domain): Domain = other match {
    case o: IntervalDomain if !emptyIntersection(other) =>
      new IntervalDomain(math.min(lb, o.lb), math.max(ub, o.ub))

    case o => super.union(other)
  }
}
