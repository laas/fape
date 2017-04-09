package fr.laas.fape.constraints.meta.types

import fr.laas.fape.constraints.meta.domains.Domain

class DomainView[T](domain: Domain, typ: Type[T]) {

  def values: Iterable[T] = domain.values.map(typ.intToInstance)

  def contains(value: T): Boolean = domain.contains(typ.instanceToInt(value))

  def head: T = typ.intToInstance(domain.lb)
}
