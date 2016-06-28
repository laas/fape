package planstack.anml.model.abs

import planstack.anml.model.{LVarRef, AbstractParameterizedStateVariable}


case class AbstractFluent(sv: AbstractParameterizedStateVariable, value: LVarRef)