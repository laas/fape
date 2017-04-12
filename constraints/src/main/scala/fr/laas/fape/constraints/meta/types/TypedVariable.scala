package fr.laas.fape.constraints.meta.types

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.Constraint
import fr.laas.fape.constraints.meta.domains.{Domain, EmptyDomain, SingletonDomain}
import fr.laas.fape.constraints.meta.variables.IntVariable

class TypedVariable[T](val typ: Type[T], ref: Option[Any] = None) extends IntVariable(typ.asDomain, ref) {

  def this(ref: Any, typ: Type[T]) = this(typ, Some(ref))

  def dom(implicit csp: CSP) : DomainView[T] = new DomainView[T](domain, typ)

  def ===(instance: T) : Constraint = this === typ.instanceToInt(instance)

  def =!=(instance: T) : Constraint = this =!= typ.instanceToInt(instance)
}

class TypedVariableWithInitialDomain[T](typ: Type[T], values: Set[T], ref: Option[Any] = None)
  extends TypedVariable[T](typ, ref) {

  override val initialDomain : Domain = Domain(values.map(typ.instanceToInt))

}
