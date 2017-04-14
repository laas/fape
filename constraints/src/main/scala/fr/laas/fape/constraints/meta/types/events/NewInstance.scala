package fr.laas.fape.constraints.meta.types.events

import fr.laas.fape.constraints.meta.events.CSPEvent
import fr.laas.fape.constraints.meta.types.dynamics.DynamicType

case class NewInstance[T](dynType: DynamicType[T], instance: T, value: Int) extends CSPEvent {
  require(!dynType.isStatic)
  require(dynType.subTypes.isEmpty)
}
