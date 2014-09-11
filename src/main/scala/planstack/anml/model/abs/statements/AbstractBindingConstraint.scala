package planstack.anml.model.abs.statements

import planstack.anml.model._
import planstack.anml.model.concrete.statements._

abstract class AbstractBindingConstraint(id : LStatementRef)
  extends AbstractStatement(id)
{
  override def bind(context: Context, pb: AnmlProblem): BindingConstraint

  override def isTemporalInterval = false
}

class AbstractAssignmentConstraint(val sv : AbstractParameterizedStateVariable, val variable : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint(id)
{
  require(sv.func.isConstant)

  override def toString = "%s := %s".format(sv, variable)


  override def bind(context: Context, pb: AnmlProblem) =
    new AssignmentConstraint(sv.bind(context), context.getGlobalVar(variable))
}

class AbstractEqualityConstraint(val sv : AbstractParameterizedStateVariable, val variable : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint(id)
{
  require(sv.func.isConstant)

  override def toString = "%s == %s".format(sv, variable)

  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  override def bind(context: Context, pb: AnmlProblem) =
    new EqualityConstraint(sv.bind(context), context.getGlobalVar(variable))
}

class AbstractVarEqualityConstraint(val leftVar : LVarRef, val rightVar : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint(id)
{
  override def toString = "%s == %s".format(leftVar, rightVar)

  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  override def bind(context: Context, pb: AnmlProblem) =
    new VarEqualityConstraint(context.getGlobalVar(leftVar), context.getGlobalVar(rightVar))
}

class AbstractInequalityConstraint(val sv : AbstractParameterizedStateVariable, val variable : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint(id)
{
  require(sv.func.isConstant)

  override def toString = "%s != %s".format(sv, variable)

  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  override def bind(context: Context, pb: AnmlProblem) =
    new InequalityConstraint(sv.bind(context), context.getGlobalVar(variable))
}

class AbstractVarInequalityConstraint(val leftVar : LVarRef, val rightVar : LVarRef, id:LStatementRef)
  extends AbstractBindingConstraint(id)
{
  override def toString = "%s != %s".format(leftVar, rightVar)


  /**
   * Produces the corresponding concrete statement, by replacing all local variables
   * by the global ones defined in Context
   * @param context Context in which this statement appears.
   * @return
   */
  override def bind(context: Context, pb: AnmlProblem) =
    new VarInequalityConstraint(context.getGlobalVar(leftVar), context.getGlobalVar(rightVar))
}
