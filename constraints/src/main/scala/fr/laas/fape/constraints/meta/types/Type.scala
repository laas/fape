package fr.laas.fape.constraints.meta.types

import fr.laas.fape.constraints.meta.domains.{Domain, EnumeratedDomain}

/**
  *
  * @tparam T Type of the instances
  */
trait Type[+T] {

  /** All instances of this type */
  def instances: Seq[T]

  /** Retrieves the int representation of given instance of this type.
    * Each instance must have a distinct int value. */
  def instanceToInt[ST >: T](instance: ST) : Int

  /** Retrieves the instance associated with tis type. */
  def intToInstance(value: Int) : T

  /** Returns true if the given instance is part of this type. */
  def hasInstance[ST >: T](instance: ST) = instances.contains(instance)

  /** Returns true if this type has instance with the given int representation. */
  def hasValue(value: Int) : Boolean

  def asDomain: Domain = new EnumeratedDomain(instances.map(instanceToInt(_)))

  def viewOf(dom: Domain) : DomainView[T] = new DomainView[T](dom, this)
}



