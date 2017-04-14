package fr.laas.fape.constraints.meta.types.dynamics

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.types.events.NewInstance
import fr.laas.fape.constraints.meta.types.statics.Type
import fr.laas.fape.constraints.meta.util.Assertion._

/** A Dynamic type represents a set of instances (and their associated int values) that can evolve over time.
  * Hence they provide method to access a static view of their current instances that is stored in the CSP. */
trait DynamicType[+T] {

  def isStatic : Boolean

  def subTypes : Seq[DynamicType[T]]

  def static(implicit csp: CSP) : Type[T] = {
    assert1(!isStatic, "Static types should override this method.")
    assert1(subTypes.isEmpty, "Composed types should override this method.")
    csp.types.asStaticType(this)
  }

  def addInstance[ST >: T](instance: ST, value: Int)(implicit csp: CSP) = {
    assert1(!isStatic, "Cannot add an instance to a static type.")
    assert1(subTypes.isEmpty, "Cannot add an instance to a composed type.")
    csp.addEvent(NewInstance(this, instance.asInstanceOf[T], value))
  }

  /** Initial set of instances, provided as a static type. */
  def defaultStatic: Type[T]
}
