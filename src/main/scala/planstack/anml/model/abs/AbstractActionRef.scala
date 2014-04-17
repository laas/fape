package planstack.anml.model.abs

import planstack.anml.{ANMLException, parser}
import planstack.anml.model._
import planstack.anml.model.abs.time.AbstractTemporalAnnotation
import planstack.anml.model.abs.statements.AbstractPersistence

/** Reference to an action as it appears in a dcomposition
  *
  * @param name Name of the action
  * @param args Parameters of the action as instances of local variables
  * @param localId Local reference to the action.
  */
class AbstractActionRef(val name:String, val args:List[LVarRef], val localId:LActRef) {
  require(localId nonEmpty)
  require(name nonEmpty)
}

object AbstractActionRef {

  /** Produces an abstract action ref and its associated TemporalStatements.
    * If some parameters appear to be functions (and not variables) Persistence statements are produces
    *
    * The temporal statements derive from parameters given as functions and not variables
    * @param pb Problem in which the reference appears.
    * @param context Partial context (such as the containing abstract action) in which the action reference appears.
    * @param ar The parsed action reference from which to create the abstract action reference.
    * @return The abstract action reference and a list of temporal statement to be enforced.
    */
  def apply(pb:AnmlProblem, context:PartialContext, ar:parser.ActionRef) : Pair[AbstractActionRef, List[AbstractTemporalStatement]] = {

    // for every argument, get a variable name and, optionally, a temporal persistence if
    // the argument was given in the form of a function
    val args : List[Pair[LVarRef, Option[AbstractTemporalStatement]]] = ar.args map(argExpr => {
      argExpr match {
        case vExpr:parser.VarExpr => {
          // argument is a variable
          val v = new LVarRef(vExpr.variable)
          if(!context.contains(v)) {
            throw new ANMLException("Error: %s is not defined.".format(v))
          }
          (v, None)
        }
        case f:parser.FuncExpr => {
          // this is a function f, create a new var v and add a persistence [all] f == v;
          val varName = new LVarRef()
          val ts = new AbstractTemporalStatement(
            AbstractTemporalAnnotation("start","end"),
            new AbstractPersistence(AbstractParameterizedStateVariable(pb, context, f), varName, new LStatementRef()))
          context.addUndefinedVar(varName, ts.statement.sv.func.valueType)
          (varName, Some(ts))
        }
      }
    })

    val actionRefId =
      if(ar.id.nonEmpty)
        new LActRef(ar.id)
      else
        new LActRef()

    context.addUndefinedAction(actionRefId)

    val statements = args.map(_._2).flatten

    Pair(new AbstractActionRef(ar.name, args.map(_._1), actionRefId), statements)
  }

}
