package fr.laas.fape.constraints.meta.types.statics

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.domains.Domain




class TypedVariableWithInitialDomain[T](typ: Type[T], values: Set[T], ref: Option[Any] = None)
  extends TypedVariable[T](typ, ref) {

  override def initialDomain(implicit csp: CSP) : Domain = Domain(values.map(typ.instanceToInt))
}
