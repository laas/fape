package planstack.anml.model

object LocalRef {
  type T = String
  val NullID : T = ""
  private var next = 0
  def getNext = {next+=1; "locRef$"+(next-1)}
}

import planstack.anml.model.LocalRef._

class LocalRef(val id:T) {
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
      this.id == o.asInstanceOf[LocalRef].id
    }
  }
}

/** Local reference to an action */
class LActRef(id:T) extends LocalRef(id) {
  require(id != NullID)

  def this() = this(getNext)
}

object LActRef {
  def apply(id:T) =
    if(id == NullID) new LActRef()
    else new LActRef(id)
}

/** Local reference to a variable */
class LVarRef(id:T) extends LocalRef(id) {
  def this() = this(getNext)
}

object LVarRef {
  def apply(id:T) =
    if(id == NullID) new LVarRef()
    else new LVarRef(id)
}

/** Local reference to a statement */
class LStatementRef(id:T) extends LocalRef(id) {
  def this() = this(getNext)
}

object LStatementRef {
  def apply(id:T) =
    if(id == NullID) new LStatementRef()
    else new LStatementRef(id)
}



