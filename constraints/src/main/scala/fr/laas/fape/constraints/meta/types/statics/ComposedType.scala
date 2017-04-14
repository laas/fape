package fr.laas.fape.constraints.meta.types.statics

import fr.laas.fape.constraints.meta.util.Assertion._

/** A high level type as a union of several subtypes. */
class ComposedType[+T](val directSubTypes: Seq[Type[T]]) extends Type[T] {

  override final def subTypes: Seq[Type[T]] = directSubTypes ++ directSubTypes.flatMap(_.subTypes)

  def instances: Seq[T] = {
    val all = directSubTypes.flatMap(st => st.instances).toList
    assert1(all.distinct.size == all.size, s"Some instances are duplicated in type $this")
    assert1(all.map(instanceToInt).distinct.size == all.size, s"Some instances have the same value in type $this")
    all
  }

  def instanceToInt[ST >: T](instance: ST) = {
    directSubTypes.find(t => t.hasInstance(instance)) match {
      case Some(t) => t.instanceToInt(instance)
      case None => throw new RuntimeException(s"Instance $instance is not part of this Type")
    }
  }

  def intToInstance(value: Int) = {
    directSubTypes.find(t => t.hasValue(value)) match {
      case Some(t) => t.intToInstance(value)
      case None => throw new RuntimeException(s"Value $value is not part of this Type")
    }
  }

  /** Returns true if this type has instance with the given int representation. */
  override def hasValue(value: Int): Boolean = directSubTypes.exists(_.hasValue(value))

  override def withoutInstance[ST >: T](instance: ST): Type[T] =
    throw new RuntimeException("Cannot remove an instance from a composed type.")

  override def withInstance[ST >: T](instance: ST, value: Int): Type[T] =
    throw new RuntimeException("Cannot add an instance to a composed type.")

  override def defaultStatic: Type[T] = this
}
