package fr.laas.fape.constraints.meta.types.statics

import fr.laas.fape.constraints.meta.util.Assertion._

/**
  * A simple type implementation.
  *
  * @param name Name of the type
  * @param _instances A Seq of tuple associating each value to its int value.
  * @tparam T Type of the instances
  */
class BaseType[T](val name: String, _instances: Seq[(T,Int)]) extends Type[T] {

  private val _instancesToInt = _instances.toMap[T, Int]
  private val _intToInstances = _instances.map(_.swap).toMap

  override val instances: Seq[T] = _instances.map(_._1)

  override def subTypes = Nil

  override def instanceToInt[ST >: T](instance: ST): Int =
    if(hasInstance(instance))
    _instancesToInt(instance.asInstanceOf[T])
    else
      throw new IllegalArgumentException

  override def intToInstance(value: Int): T = _intToInstances(value)

  override def hasValue(value: Int): Boolean = _intToInstances.contains(value)

  override def toString = name

  override def withInstance[ST >: T](instance: ST, value: Int): Type[T] = {
    assert1(!hasInstance(instance), s"Type $this already has an instance $instance")
    assert1(!hasValue(value), s"Type $this already has a value $value")
    new BaseType[T](name, (instance.asInstanceOf[T], value) :: _instances.toList)
  }

  override def withoutInstance[ST >: T](instance: ST): Type[T] = {
    assert1(hasInstance(instance), s"Type $this does not have an instance $instance")
    new BaseType[T](name, _instances.filter(p => p._1 != instance))
  }

  override def defaultStatic: Type[T] = this
}

object BaseType {

  /** Factory in which instances are given a default int value starting from 0 */
  def apply[T](name: String, instances: Seq[T], intValueOffset: Int = 0) =
    new BaseType[T](name, instances.zipWithIndex.map(p => (p._1, p._2+intValueOffset)))

}
