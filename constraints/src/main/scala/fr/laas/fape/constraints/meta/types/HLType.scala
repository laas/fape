package fr.laas.fape.constraints.meta.types

import fr.laas.fape.constraints.meta.util.Assertion._

trait HLType[+T] extends Type[T] {



  def ownInstances: Seq[T]

  def ownInstanceToInt[ST >: T](value: ST) : Int
  def intToOwnInstance(value: Int) : T

  def subTypes: Seq[Type[T]]

  def instances: Seq[T] = {
    val all = (ownInstances ++ subTypes.flatMap(st => st.instances)).toList
    assert1(all.distinct.size == all.size, "Some instances are duplicated in this type")
    assert1(all.map(instanceToInt).distinct.size == all.size)
    all
  }

  def instanceToInt[ST >: T](instance: ST) = {
    if(ownInstances.contains(instance))
      ownInstanceToInt(instance)
    else
      subTypes.find(t => t.hasInstance(instance)) match {
        case Some(t) => t.instanceToInt(instance)
        case None => throw new RuntimeException(s"Instance $instance is not part of this Type")
      }
  }

  def intToInstance(value: Int) = {
    if(ownInstances.map(ownInstanceToInt(_)).contains(value))
      intToOwnInstance(value)
    else
      subTypes.find(t => t.hasValue(value)) match {
        case Some(t) => t.intToInstance(value)
        case None => throw new RuntimeException(s"Value $value is not part of this Type")
      }
  }
}
