package fr.laas.fape.constraints.meta.types.dynamics

import fr.laas.fape.constraints.meta.types.statics.{BaseType, Type}

class BaseDynamicType[T](val name: String, initialValues: Seq[(T, Int)]) extends DynamicType[T] {

  override def isStatic: Boolean = false

  override def subTypes: Seq[DynamicType[T]] = Nil

  override lazy val defaultStatic: Type[T] = new BaseType[T](name, initialValues)
}
