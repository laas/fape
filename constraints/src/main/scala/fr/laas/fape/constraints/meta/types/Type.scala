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

  def hasInstance[ST >: T](instance: ST) = instances.contains(instance)
  def hasValue(value: Int) : Boolean = hasInstance(intToInstance(value))

  def asDomain: Domain = new EnumeratedDomain(instances.map(instanceToInt(_)))
}



