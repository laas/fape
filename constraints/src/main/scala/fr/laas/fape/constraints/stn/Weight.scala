package fr.laas.fape.constraints.stn

class Weight(val inf : Boolean, val w : Int) {

  def this() = this(true, 0)
  def this(w: Int) = this(false, w)

  def + (o: Weight) : Weight = {
    if(inf || o.inf)
      Weight.InfWeight
    else
      new Weight(w + o.w)
  }

  def + (o: Int) : Weight = {
    if(inf)
      this
    else
      new Weight(w + o)
    }

  def > (o: Weight) : Boolean = {
    if(o.inf)
      false
    else if(inf)
      true
    else
      w > o.w
  }

  def > (that:Int) : Boolean = {
    if(inf)
      true
    else
      w > that
  }

  def < (o: Weight) : Boolean = {
    if(inf)
      false
    else if(o.inf)
      true
    else
      w < o.w
  }

  def < (o: Int) : Boolean = {
    if(inf)
      false
    else
      w < o
  }

  def == (o:Weight) : Boolean = {
    if(inf) o.inf
    else if(o.inf) false
    else w == o.w
  }

  def <= (o:Weight) : Boolean = this < o || this == o

  override def toString = {
    if(inf) "inf"
    else w.toString
  }
}

object Weight {
  val InfWeight = new Weight
}
