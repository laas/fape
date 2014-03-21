package planstack.anml.model.concrete

object GlobalRef {
  type T = Integer

  val NullID : T = -1
  var next = 0

  def getNext = {next += 1; next-1}
}

import GlobalRef._

class GlobalRef(val id:T) {
  def this() = this(getNext)

  def isEmpty = id == NullID
  def nonEmpty = !isEmpty

  override def toString = id.toString

  override val hashCode = id.hashCode

  override def equals(o:Any) = {
    if(o.isInstanceOf[T]) {
      id == o
    } else if(this.getClass != o.getClass) {
      false
    } else {
      this.id == o.asInstanceOf[GlobalRef].id
    }
  }
}


class ActRef(id:T) extends GlobalRef(id) {
  def this() = this(getNext)
}

object EmptyActRef extends ActRef(NullID)

class VarRef(id:T) extends GlobalRef(id) {
  def this() = this(getNext)
}

object EmptyVarRef extends VarRef(NullID)

class TPRef(id:T) extends GlobalRef(id) {
  def this() = this(getNext)
}


object EmptyGlobalRef extends GlobalRef(NullID)