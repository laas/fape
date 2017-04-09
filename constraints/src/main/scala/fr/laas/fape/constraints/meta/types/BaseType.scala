package fr.laas.fape.constraints.meta.types

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

  override def instanceToInt[ST >: T](instance: ST): Int =
    if(hasInstance(instance))
    _instancesToInt(instance.asInstanceOf[T])
    else
      throw new IllegalArgumentException

  override def intToInstance(value: Int): T = _intToInstances(value)

  override def toString = name
}

object BaseType {

  /** Factory in which instances are given a default int value */
  def apply[T](name: String, instances: Seq[T]) =
    new BaseType[T](name, instances.zipWithIndex)

}
