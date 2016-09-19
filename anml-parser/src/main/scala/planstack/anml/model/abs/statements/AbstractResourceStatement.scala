package planstack.anml.model.abs.statements

import planstack.anml.model.concrete.statements._
import planstack.anml.model.concrete.{Chronicle, RefCounter}
import planstack.anml.model._

/** An abstract ANML resource Statement.
  *
  * It applies on a parameterized state variable and takes integer parameters.
  * Further work should be done to support more complex right hand side expressions.
  *
  * @param sv State Variable on which this statement applies
  * @param param Right side of the statement: a numeric value. For instance, in the statement `energy :use 50`, 50 would be the param.
  * @param id A local reference to the Statement (for temporal constraints).
  */
abstract class AbstractResourceStatement(val sv:AbstractParameterizedStateVariable, val param:Float, override val id:LStatementRef)
  extends AbstractStatement(id)
{
  require(sv.func.valueType.isNumeric, "Functions in resource statements must have an integer value type: "+sv)

  def operator : String

  override def toString = "%s : %s %s %s".format(id, sv, operator, param)

  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  override def bind(context: Context, pb:AnmlProblem, container: Chronicle, refCounter: RefCounter): ResourceStatement = {
    val variable = sv.bind(context)

    this match {
      case _:AbstractProduceResource => new ProduceResource(variable, param, container, refCounter)
      case _:AbstractSetResource => new SetResource(variable, param, container, refCounter)
      case _:AbstractConsumeResource => new ConsumeResource(variable, param, container, refCounter)
      case _:AbstractLendResource => new LendResource(variable, param, container, refCounter)
      case _:AbstractUseResource => new UseResource(variable, param, container, refCounter)
      case _:AbstractRequireResource => new RequireResource(variable, operator, param, container, refCounter)
    }
  }

  override def getAllVars: Set[LVarRef] = sv.getAllVars
}

class AbstractProduceResource(sv:AbstractParameterizedStateVariable, param:Float, id:LStatementRef) extends AbstractResourceStatement(sv, param, id) {
  val operator = ":produce"
}

class AbstractSetResource(sv:AbstractParameterizedStateVariable, param:Float, id:LStatementRef) extends AbstractResourceStatement(sv, param, id) {
  val operator = ":="
}

class AbstractLendResource(sv:AbstractParameterizedStateVariable, param:Float, id:LStatementRef) extends AbstractResourceStatement(sv, param, id) {
  val operator = ":lend"
}

class AbstractUseResource(sv:AbstractParameterizedStateVariable, param:Float, id:LStatementRef) extends AbstractResourceStatement(sv, param, id) {
  val operator = ":use"
}

class AbstractConsumeResource(sv:AbstractParameterizedStateVariable, param:Float, id:LStatementRef) extends AbstractResourceStatement(sv, param, id) {
  val operator = ":consume"
}

class AbstractRequireResource(sv:AbstractParameterizedStateVariable, val operator:String, param:Float, id:LStatementRef) extends AbstractResourceStatement(sv, param, id) {
  require(Set("<=","<",">=",">").contains(operator))
}