package fr.laas.fape.constraints.meta.types.statics

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.domains.{Domain, EnumeratedDomain}
import fr.laas.fape.constraints.meta.types.dynamics.DynamicType

/** A static type composed of a set of instances and a mapping from
  * domain values to instances (and vice versa).
  *
  * @tparam T Type of the instances
  */
trait Type[+T] extends DynamicType[T] {

  override def isStatic = true

  override def static(implicit csp: CSP) = this

  override def subTypes : Seq[Type[T]]

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

  def asDomain: Domain = Domain(instances.map(instanceToInt(_)).toSet)

  def viewOf(dom: Domain) : DomainView[T] = new DomainView[T](dom, this)

  /** Returns a new version of this type with an additional instance. */
  def withInstance[ST >: T](instance: ST, value: Int) : Type[T]

  /** Returns a new version of this type without the given instance. */
  def withoutInstance[ST >: T](instance: ST) : Type[T]
}
