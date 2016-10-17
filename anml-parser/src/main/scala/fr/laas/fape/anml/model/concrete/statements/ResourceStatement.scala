package fr.laas.fape.anml.model.concrete.statements

import fr.laas.fape.anml.model.{NumFunction, ParameterizedStateVariable}
import fr.laas.fape.anml.model.concrete.{Chronicle, RefCounter, TPRef}



/** A concrete ANML resource Statement.
  *
  * It applies on a parameterized state variable and takes integer parameters.
  * Further work should be done to support more complex right hand side expressions.
  *
  * Subclasses and the `operator` method gives more details on the type of statement.
  *
  * @param sv State Variable on which this statement applies
  * @param param Right side of the statement: a numeric value. For instance, in the statement `energy :use 50`, 50 would be the param.
  */
abstract class ResourceStatement(sv:ParameterizedStateVariable, val param:Float, container:Chronicle, refCounter: RefCounter) extends Statement(sv, container) {
  require(sv.func.isInstanceOf[NumFunction], "Error: this resource statement is not applied " +
    "on a numeric function: "+this)

  override val start : TPRef = new TPRef(refCounter)
  override val end : TPRef = new TPRef(refCounter)

  def operator :String

  override def toString = "%s %s %s".format(sv, operator, param)
}


class SetResource(sv :ParameterizedStateVariable, param :Float, container:Chronicle, refCounter: RefCounter)
  extends ResourceStatement(sv, param, container, refCounter) {
  val operator: String = ":="
}

class UseResource(sv :ParameterizedStateVariable, param :Float, container:Chronicle, refCounter: RefCounter)
  extends ResourceStatement(sv, param, container, refCounter) {
  val operator: String = ":use"
}

class ConsumeResource(sv :ParameterizedStateVariable, param :Float, container:Chronicle, refCounter: RefCounter)
  extends ResourceStatement(sv, param, container, refCounter) {
  val operator: String = ":consume"
}

class LendResource(sv :ParameterizedStateVariable, param :Float, container:Chronicle, refCounter: RefCounter)
  extends ResourceStatement(sv, param, container, refCounter) {
  val operator: String = ":lend"
}

class ProduceResource(sv :ParameterizedStateVariable, param :Float, container:Chronicle, refCounter: RefCounter)
  extends ResourceStatement(sv, param, container, refCounter) {
  val operator: String = ":produce"
}

/** Defines a condition on the resource such as <, >, <= or >=
  *
  * @param sv State Variable on which this statement applies
  * @param operator The operator: <, >, <= or >=
  * @param param Right side of the statement: a numeric value. For instance, in the statement `energy :use 50`, 50 would be the param.
  */
class RequireResource(sv :ParameterizedStateVariable, val operator :String, param :Float, container:Chronicle, refCounter: RefCounter)
  extends ResourceStatement(sv, param, container, refCounter) {
  assert(Set("<=",">=","<",">").contains(operator))
}
