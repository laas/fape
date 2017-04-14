package fr.laas.fape.planning.types

import fr.laas.fape.anml.model.SymFunction
import fr.laas.fape.constraints.meta.types.statics.{BaseType, Type}

class FunctionVarType(val functions: Iterable[SymFunction])
  extends BaseType[SymFunction]("TFunction", functions.toList.zipWithIndex) {

}
