package fr.laas.fape.constraints.meta.types

class SuperType[+T](val subTypes: Seq[Type[T]]) extends HLType[T]{

  override def ownInstances: Seq[T] = Nil

  override def ownInstanceToInt[ST >: T](value: ST): Int = ???

  override def intToOwnInstance(value: Int): T = ???

  override def hasValue(value: Int): Boolean = subTypes.exists(_.hasValue(value))
}
