package fr.laas.fape.constraints.meta.types

import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.util.Assertion._

class DomainView[+T](domain: Domain, typ: Type[T]) {

  def size = domain.size

  def values: Iterable[T] = domain.values.map(typ.intToInstance)

  def contains[ST >: T](instance: ST): Boolean = {
    assert1(typ.hasInstance(instance), s"Instance $instance is not par of type $typ")
    domain.contains(typ.instanceToInt(instance))
  }

  def head: T = typ.intToInstance(domain.lb)

  override def toString : String =
    if(size == 1)
      values.head.toString
    else if(size <= 6)
      s"{${values.mkString(", ")}}"
    else
      s"{${values.take(3).mkString(", ")}, ..., ${values.takeRight(3)}}"
}
