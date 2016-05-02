package planstack.anml.model

import planstack.anml.{FunctionNotFoundInType, ANMLException}
import planstack.anml.model.concrete.InstanceRef

import scala.collection.mutable

trait Type

class SimpleType(val name:String, val parent:Option[SimpleType]) extends Type {
  private val _methods = mutable.Set[String]()
  private val _instances = mutable.Set[InstanceRef]()
  private val _children = mutable.Set[SimpleType]()
  parent.foreach(p => p.addDirectSubType(this))

  private[model] def addMethod(methodName: String) {
    _methods += methodName
  }

  private[model] def addInstance(instance: InstanceRef) {
    _instances += instance
    parent.foreach(p => p.addInstance(instance))
  }

  private[model] def addDirectSubType(typ: SimpleType): Unit = {
    _children += typ
  }

  def parents: collection.Set[Type] = parent.toSet.flatMap((p:SimpleType) => p.parents) ++ parent.toSet

  def instances: collection.Set[InstanceRef] = _instances

  def allSubTypes : collection.Set[SimpleType] = _children.flatMap(_.allSubTypes) + this

  def getQualidiedFunction(funcName: String): String = {
    try {
      if (_methods.contains(funcName))
        name + "." + funcName
      else parent match {
        case Some(p) => p.getQualidiedFunction(funcName)
        case None => throw new FunctionNotFoundInType(funcName, name)
      }
    } catch {
      case e: FunctionNotFoundInType => throw new FunctionNotFoundInType(funcName, name)
    }
  }

  override def toString = name
}

case class UnionType(val parts:Set[Type])


