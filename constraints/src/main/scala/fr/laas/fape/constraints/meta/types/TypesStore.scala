package fr.laas.fape.constraints.meta.types

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.events.{CSPEventHandler, Event, InternalCSPEventHandler, NewVariableEvent}
import fr.laas.fape.constraints.meta.types.dynamics.{DynTypedVariable, DynamicType}
import fr.laas.fape.constraints.meta.types.events.NewInstance
import fr.laas.fape.constraints.meta.types.statics.Type
import fr.laas.fape.constraints.meta.util.Assertion._

import scala.collection.mutable

class TypesStore(_csp: CSP, base: Option[TypesStore] = None) extends InternalCSPEventHandler {

  implicit val csp = _csp

  private val dynamicToStatic: mutable.Map[DynamicType[Any], Type[Any]] = base match {
    case Some(prev) => prev.dynamicToStatic.clone()
    case None => mutable.Map()
  }

  private val varsByType: mutable.Map[DynamicType[_], mutable.ArrayBuffer[DynTypedVariable[_]]] = base match {
    case Some(prev) => prev.varsByType.clone()
    case None => mutable.Map()
  }

  def asStaticType[T](dynamicType: DynamicType[T]) : Type[T] =
    dynamicToStatic.getOrElse(dynamicType, dynamicType.defaultStatic).asInstanceOf[Type[T]]

  override def handleEvent(event: Event) {
    event match {
      case NewVariableEvent(v: DynTypedVariable[_]) =>
        if(!v.typ.isStatic) {
          if(!dynamicToStatic.contains(v.typ) && v.typ.subTypes.isEmpty)
            dynamicToStatic.put(v.typ, v.typ.defaultStatic)
          for(t <- v.typ :: v.typ.subTypes.toList if !t.isStatic && t.subTypes.isEmpty)
            varsByType.getOrElseUpdate(t, mutable.ArrayBuffer()) += v
        }

      case NewInstance(dynType, instance, value) =>
        assert1(!dynType.isStatic)
        assert1(dynType.subTypes.isEmpty, s"Cannot add instances to a non primitive type")
        if(!dynamicToStatic.contains(dynType))
          dynamicToStatic.put(dynType, dynType.defaultStatic)
        assert2(dynamicToStatic(dynType) == dynType.static)
        assert1(!dynType.static.hasInstance(instance), s"Type $dynType already has an instance $instance")
        assert1(!dynType.static.hasValue(value), s"Type $dynType already has a value $value")
        dynamicToStatic(dynType) = dynamicToStatic(dynType).withInstance(instance, value)
        for(v <- varsByType.getOrElse(dynType, Nil))
          csp.updateDomain(v, v.domain + value)

      case _ =>
    }
  }


  /** Invoked when a CSP is cloned, the new CSP will append the handler resulting from this method into its own handlers */
  override def clone(newCSP: CSP): InternalCSPEventHandler = new TypesStore(newCSP, Some(this))
}
