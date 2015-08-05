package planstack.anml.model.concrete

class RefCounter(protected var nextTPRef:Int, protected var nextActRef:Int, protected var nextVarRef:Int) {

  def this() = this(0,0,0)
  def this(refCounter: RefCounter) = this(refCounter.nextTPRef, refCounter.nextActRef, refCounter.nextVarRef)

  def nextTP() : Int = { nextTPRef += 1 ; nextTPRef -1 }
  def nextAct() : Int = { nextActRef += 1 ; nextActRef -1 }
  final def nextVar() : Int = { nextVarRef += 1  ; nextVarRef -1 }
}

object GlobalRef {
  type T = Int

  val NullID : T = -1
  var next = 0

  def getNext = {next += 1; next-1}
}

import planstack.anml.model.concrete.GlobalRef._

/** Global reference to an anml object.
  *
  * @param id Unique id of the reference.
  */
class GlobalRef(val id:T) extends planstack.UniquelyIdentified {
  require(id<next, "Error: ids should be strictly growing to avoid overlapping.")
  def this() = this(getNext)

  private var placeHolderID = -1
  final def selfDefinedID = { assert(placeHolderID != -1) ; placeHolderID }
  final def setSelfDefinedID(n: Int) { require(n != -1) ; assert(placeHolderID == -1) ; placeHolderID = n }
  final def hasDefinedID = placeHolderID != -1

  final def isEmpty = id == NullID
  final def nonEmpty = !isEmpty

  override def toString = id.toString

  override final val hashCode = id

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
  def this(refCounter: RefCounter) = this(refCounter.nextAct())
}

object EmptyActRef extends ActRef(NullID)

/** Reference to a concrete variable (those typically appear as parameters of state variables and in
  * binding constraints).
  * @param id Unique id of the reference.
  */
class VarRef(id:T) extends GlobalRef(id) { //TODO: this should be typed
  def this(refCounter: RefCounter) = this(refCounter.nextVar())
}

/** Reference to a problem instance that takes the form of a variable.
  *
  * This is mainly to be able to treat instances as variables (e.g. parameters for
  * state variables, actions, ...).
  *
  * @param id Unique id of the reference.
  * @param instance Name of the instance.
  */
class InstanceRef(id:T, val instance:String) extends VarRef(id) {
  def this(instance :String, refCounter: RefCounter) = this(refCounter.nextVar(), instance)

  override def toString = instance
}

object EmptyVarRef extends VarRef(NullID)

/** Reference to a time-point: an temporal variable typically denoting the start or end time of an action
  * and that appears in Simple Temporal Problems.
  * @param id Unique id of the reference.
  */
class TPRef(id:T) extends GlobalRef(id) {
  /** Creates a new reference with a unique (not given yet) ID) */
  def this(refCounter: RefCounter) = this(refCounter.nextTP())

  private var typ = 0
  final def isVirtual = typ == 1
  final def isContingent = typ == 2
  final def isControllable = typ == 3
  final def isOfUndefinedType = typ == 0
  final def setVirtual() { assert(typ == 0) ; typ = 1 }
  final def setContingent() { assert(typ == 0) ; typ = 2 }
  final def setControllable() { assert(typ == 0) ; typ = 3 }
}


object EmptyGlobalRef extends GlobalRef(NullID)