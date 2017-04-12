package fr.laas.fape.planning.variables

import fr.laas.fape.anml.model
import fr.laas.fape.anml.model.{ParameterizedStateVariable, SymFunction}
import fr.laas.fape.anml.model.concrete.{InstanceRef, VarRef}
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.types.{TypedVariable, TypedVariableWithInitialDomain}
import fr.laas.fape.constraints.meta.variables.VariableSeq
import fr.laas.fape.planning.types.{AnmlVarType, FunctionVarType}

/** Any ANML variable (e.g. action parameters or declared with constant) */
class Var(v: VarRef, typ: AnmlVarType) extends TypedVariable[String](typ, Some(v))

/** Variable that represents a particular instance. Its domain is a singleton containing this particular instance. */
class InstanceVar(v: InstanceRef, typ: AnmlVarType) extends Var(v, typ) {
  override val initialDomain : Domain = Domain(Set(typ.instanceToInt(v.instance)))
}

/** A variable representing a particular ANML function (e.g. the "position" of "position(x)".
  * Its domain is always a singleton, containing the SymFunction itself. */
class FVar(val f: model.SymFunction, typ: FunctionVarType)
  extends TypedVariableWithInitialDomain[SymFunction](typ, Set(f), Some(f))

/** A state variable, defined as a sequence of an ANML function (FVar) and parameters of the state variable */
class SVar(func: FVar, params: Seq[Var], ref: ParameterizedStateVariable)
  extends VariableSeq(func :: params.toList, Some(ref)) {
  require(func.f.argTypes.size == params.size)
}