package fr.laas.fape.planning.types

import fr.laas.fape.anml.model.SimpleType
import fr.laas.fape.constraints.meta.types.statics.{BaseType, Type}

class AnmlVarType(name: String, val anmlType: SimpleType)
  extends BaseType[String](name, anmlType.instances.toList.map(i => (i.instance, i.id))) {

}
