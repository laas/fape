package planstack.anml.model.concrete

object GlobalRef {
  type T = Integer

  val NullID : T = -1
  var next = 0

  def getNext = {next += 1; next-1}
}

import GlobalRef._

/** Global reference to an anml object.
  *
  * @param id Unique id of the reference.
  */
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

/** Reference to a concrete [[planstack.anml.model.concrete.Action]]. */
class ActRef(id:T) extends GlobalRef(id) {

  /** Builds a new ActRef with a new unique ID */
  def this() = this(getNext)
}

object EmptyActRef extends ActRef(NullID)

/** Reference to a concrete variable (those typically appear as parameters of state variables and in
  * binding constraints).
  * @param id Unique id of the reference.
  */
class VarRef(id:T) extends GlobalRef(id) {
  def this() = this(getNext)
}

object EmptyVarRef extends VarRef(NullID)

/** Reference to a time-point: an temporal variable typically denoting the start or end time of an action
  * and that appears in Simple Temporal Problems.
  * @param id Unique id of the reference.
  */
class TPRef(id:T) extends GlobalRef(id) {
  def this() = this(getNext)
}


object EmptyGlobalRef extends GlobalRef(NullID)