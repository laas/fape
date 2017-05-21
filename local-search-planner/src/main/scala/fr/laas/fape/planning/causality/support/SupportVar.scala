package fr.laas.fape.planning.causality.support

import fr.laas.fape.constraints.meta.types.dynamics.{DynTypedVariable, DynamicType}
import fr.laas.fape.planning.causality.SupportOption

class SupportVar(t: DynamicType[SupportOption]) extends DynTypedVariable[SupportOption](t) {

  override def isDecisionVar = false

  override def toString = "support-var@"+hashCode()
}
