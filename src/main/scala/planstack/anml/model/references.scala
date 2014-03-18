package planstack.anml.model



object Ref {
  type T = String
}

import Ref._

abstract class Ref(val id:T) {

  def isEmpty = id.isEmpty
  def nonEmpty = !isEmpty

  override def toString = id.toString

  override val hashCode = id.hashCode

  override def equals(o:Any) = {
    if(o.isInstanceOf[T]) {
      id == o
    } else if(this.getClass != o.getClass) {
      false
    } else {
      this.id == o.asInstanceOf[Ref].id
    }
  }

}

class LocalRef(id:T) extends Ref(id)

class LActRef(id:T) extends LocalRef(id) {
  require(id.nonEmpty)
}
class LVarRef(id:T) extends LocalRef(id)

class GlobalRef(id:T) extends Ref(id)

class ActRef(id:T) extends GlobalRef(id)
class VarRef(id:T) extends GlobalRef(id)
class TPRef(id:T) extends GlobalRef(id)


object EmptyGlobalRef extends GlobalRef("")