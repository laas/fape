package fr.laas.fape.planning.causality.support

import fr.laas.fape.constraints.meta.types.dynamics.{DynTypedVariable, DynamicType}
import fr.laas.fape.planning.causality.SupportOption
import fr.laas.fape.planning.structures.Holds

class SupportVar(t: DynamicType[SupportOption], val target: Holds) extends DynTypedVariable[SupportOption](t) {

  override def isDecisionVar = false

  override def toString = s"support-var@[$target]"
}
