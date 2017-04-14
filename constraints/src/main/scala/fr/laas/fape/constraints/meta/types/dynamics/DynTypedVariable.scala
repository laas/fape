package fr.laas.fape.constraints.meta.types.dynamics

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.types.statics.{DomainView, Type}
import fr.laas.fape.constraints.meta.variables.IntVariable

/** Variable with a dynamic type.
  * If an instance is added to the dynamic type, the domain of this variable is extended with the corresponding value. */
class DynTypedVariable[T](val typ: DynamicType[T], ref: Option[Any] = None)
  extends IntVariable(typ.defaultStatic.asDomain, ref) {

  def this(ref: Any, typ: Type[T]) = this(typ, Some(ref))

  def dom(implicit csp: CSP) : DomainView[T] = new DomainView[T](domain, typ.static)
}
