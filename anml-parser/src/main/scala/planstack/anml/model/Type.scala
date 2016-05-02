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

  private[model] def addMethod(methodName: String) { ??? }

  def isNumeric : Boolean
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

class SimpleType(val name:String, val parent:Option[SimpleType]) extends Type {
  private val _methods = mutable.Set[String]()
  private val _instances = mutable.Set[InstanceRef]()
  private val _children = mutable.Set[SimpleType]()
  parent.foreach(p => p.addDirectSubType(this))

  private[model] override def addMethod(methodName: String) {
    _methods += methodName
  }

  private[model] def addInstance(instance: InstanceRef) {
    _instances += instance
    parent.foreach(p => p.addInstance(instance))
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

  override def toString = name
}

//case class UnionType(val parts:Set[Type])


