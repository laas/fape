package fr.laas.fape.anml.model.concrete

import fr.laas.fape.anml.model.Type

class RefCounter(protected var nextTPRef:Int, protected var nextActRef:Int, protected var nextVarRef:Int) {

  def this() = this(0,0,0)
  def this(refCounter: RefCounter) = this(refCounter.nextTPRef, refCounter.nextActRef, refCounter.nextVarRef)

  def nextTP() : Int = { nextTPRef += 1 ; nextTPRef -1 }
  def nextAct() : Int = { nextActRef += 1 ; nextActRef -1 }
  final def nextVar() : Int = { nextVarRef += 1  ; nextVarRef -1 }
}

object GlobalRef {
  val NullID : Int = -1
}

import fr.laas.fape.anml.model.abs.time.TimepointType
import GlobalRef.NullID

/** Global reference to an anml object.
  *
  * @param id Unique id of the reference.
  */
class GlobalRef(val id:Int) extends planstack.UniquelyIdentified {
  private var placeHolderID = -1
  final def selfDefinedID = { assert(placeHolderID != -1) ; placeHolderID }
  final def setSelfDefinedID(n: Int) { require(n != -1) ; assert(placeHolderID == -1) ; placeHolderID = n }
  final def hasDefinedID = placeHolderID != -1

  final def isEmpty = id == NullID
  final def nonEmpty = !isEmpty

  override def toString = id.toString

  override final val hashCode = id

  override def equals(o:Any) = {
    if(this.getClass != o.getClass) {
      false
    } else {
      this.id == o.asInstanceOf[GlobalRef].id
    }
  }
}

/** Reference to a concrete [[Action]]. */
class ActRef(id:Int) extends GlobalRef(id) {

  /** Builds a new ActRef with a new unique ID */
  def this(refCounter: RefCounter) = this(refCounter.nextAct())
}

object EmptyActRef extends ActRef(NullID)

case class Label(context:String, localLabel: String) {
  override def toString = context+"."+(if(localLabel.isEmpty) "??" else localLabel)
}

class Variable(id: Int) extends GlobalRef(id)

/** Reference to a concrete variable (those typically appear as parameters of state variables and in
  * binding constraints).
  *
  * @param id Unique id of the reference.
  */
class VarRef(id:Int, val typ :Type, val label:Label) extends Variable(id) {
  def this(typ :Type, refCounter: RefCounter, label: Label) = this(refCounter.nextVar(), typ, label)

  def getType = typ
}

/** Reference to a problem instance that takes the form of a variable.
  *
  * This is mainly to be able to treat instances as variables (e.g. parameters for
  * state variables, actions, ...).
  *
  * @param id Unique id of the reference.
  * @param instance Name of the instance.
  */
class InstanceRef(id:Int, val instance:String, typ :Type) extends VarRef(id, typ, Label("problem",instance)) {
  def this(instance :String, typ :Type, refCounter: RefCounter) = this(refCounter.nextVar(), instance, typ)

  override def toString = instance
}

class EmptyVarRef(typ :Type) extends VarRef(NullID, typ, Label("??","EMPTY_VAR_REF"))

/** Reference to a time-point: an temporal variable typically denoting the start or end time of an action
  * and that appears in Simple Temporal Problems.
 *
  * @param id Unique id of the reference.
  */
class TPRef(id:Int) extends Variable(id) {
  /** Creates a new reference with a unique (not given yet) ID) */
  def this(refCounter: RefCounter) = this(refCounter.nextTP())

  val genre = new TimepointType
}


object EmptyGlobalRef extends GlobalRef(NullID)