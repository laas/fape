package fr.laas.fape.anml.model.abs

import fr.laas.fape.anml.model.{AbstractParameterizedStateVariable, LVarRef}


case class AbstractFluent(sv: AbstractParameterizedStateVariable, value: LVarRef)