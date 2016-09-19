package planstack.anml.model

import planstack.anml.{FunctionNotFoundInType, ANMLException}
import planstack.anml.model.concrete.InstanceRef

import scala.collection.mutable

import scala.collection.JavaConverters._

trait Type {
  def name : String
  def instances : collection.Set[InstanceRef] = ???
  def jInstances = instances.asJava
  def instancesAsStrings = instances.map(i => i.instance).toList.asJava
  def allSubTypes : collection.Set[Type] = ???
  def jAllSubTypes = allSubTypes.asJava
  def parents : collection.Set[Type] = ???
  def jParents = parents.asJava
  def getQualifiedFunction(funcName: String): String = ???

  def compatibleWith(otherType :Type) : Boolean = {
    this == otherType ||
      parents.contains(otherType) ||
      allSubTypes.contains(otherType)
  }

  private[model] def addMethod(methodName: String) { ??? }
  private[model] def addInstance(instance: InstanceRef) { ??? }

  def isNumeric : Boolean
  override def toString = name
}


trait NumericType extends Type {
  override def isNumeric = true
}
object TInteger extends NumericType {
  def name = "integer"
}

object TMethods extends Type {
  def name = "Methods"
  def isNumeric = false
}


object AnyType extends SimpleType("AnyType", None) {
}

class SimpleType(val name:String, val parentOpt:Option[SimpleType]) extends Type {
  val topLevel = false

  private val _methods = mutable.Set[String]()
  private val _instances = mutable.Set[InstanceRef]()
  private val _children = mutable.Set[SimpleType]()
  private val _supertypes = mutable.Set[Type]()

  def parent = parentOpt match {
    case Some(p) => Some(p)
    case None if name == "AnyType" => None
    case _ => Some(AnyType)
  }

  parent.foreach(p => {
    p.addDirectSubType(this)
    addSuperType(p)
  })

  private[model] def addSuperType(t: Type) { _supertypes += t }

  private[model] override def addMethod(methodName: String) {
    _methods += methodName
  }

  private[model] override def addInstance(instance: InstanceRef) {
    _instances += instance
    _supertypes.foreach(p => p.addInstance(instance))
  }

  private[model] def addDirectSubType(typ: SimpleType): Unit = {
    _children += typ
  }

  override def isNumeric = false

  override def parents: collection.Set[Type] = parent.toSet.flatMap((p:SimpleType) => p.parents) ++ parent.toSet

  override def instances: collection.Set[InstanceRef] = _instances

  override def allSubTypes : collection.Set[Type] = _children.toSet.flatMap((c:SimpleType) => c.allSubTypes) + this

  override def getQualifiedFunction(funcName: String): String = {
    try {
      if (_methods.contains(funcName))
        name + "." + funcName
      else parent match {
        case Some(p) => p.getQualifiedFunction(funcName)
        case None => throw new FunctionNotFoundInType(funcName, name)
      }
    } catch {
      case e: FunctionNotFoundInType => throw new FunctionNotFoundInType(funcName, name)
    }
  }
}

case class UnionType(val parts:Set[SimpleType]) extends Type {
  override def name: String = "("+parts.mkString(" or ")+")"
  override def isNumeric: Boolean = false

  // initialize instances and record ourself to sub types to get any update
  val _instances : mutable.Set[InstanceRef] = mutable.Set(parts.flatMap(s => s.instances).toArray: _*)
  for(sub <- parts) sub.addSuperType(this)

  override def addInstance(i: InstanceRef) { _instances += i }
  override def instances : collection.Set[InstanceRef] = _instances
}


