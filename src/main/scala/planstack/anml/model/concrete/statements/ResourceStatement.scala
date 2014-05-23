package planstack.anml.model.concrete.statements

import planstack.anml.model.ParameterizedStateVariable

/** A concrete ANML resource Statement.
  *
  * It applies on a parameterized state variable and takes integer parameters.
  * Further work should be done to support more complex right hand side expressions.
  *
  * @param sv State Variable on which this statement applies
  * @param param Right side of the statement: a numeric value. For instance, in the statement `energy :use 50`, 50 would be the param.
  */
abstract class ResourceStatement(sv:ParameterizedStateVariable, val param:Int) extends Statement(sv) {
  require(sv.func isInstanceOf)

  def operator :String

  override def toString = "%s %s %s".format(sv, operator, param)
}


class SetResource(sv :ParameterizedStateVariable, param :Int) extends ResourceStatement(sv, param) {
  val operator: String = ":="
}

class UseResource(sv :ParameterizedStateVariable, param :Int) extends ResourceStatement(sv, param) {
  val operator: String = ":use"
}

class ConsumeResource(sv :ParameterizedStateVariable, param :Int) extends ResourceStatement(sv, param) {
  val operator: String = ":consume"
}

class LendResource(sv :ParameterizedStateVariable, param :Int) extends ResourceStatement(sv, param) {
  val operator: String = ":lend"
}

class ProduceResource(sv :ParameterizedStateVariable, param :Int) extends ResourceStatement(sv, param) {
  val operator: String = ":produce"
}

class RequireResource(sv :ParameterizedStateVariable, val operator :String, param :Int) extends ResourceStatement(sv, param) {
  assert(Set("<=",">=","<",">").contains(operator))
}
