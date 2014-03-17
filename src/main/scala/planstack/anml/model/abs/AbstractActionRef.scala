package planstack.anml.model.abs

import planstack.anml.parser
import planstack.anml.model._

class AbstractActionRef(val name:String, val args:List[LVarRef], val localId:LActRef) {
  require(localId nonEmpty)
  require(name nonEmpty)
}

object AbstractActionRef {

  private var nextID = 0
  protected def newLocalActionRef = "lActionRef"+{nextID+=1; nextID-1}

  /** Produces an abstract action ref and its associated TemporalStatements.
    *
    * The temporal statements derive from parameters given as functions and not variables
    * @param pb
    * @param context
    * @param ar
    * @return
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
            // var doesn't exists, add it to context
            context.addUndefinedVar(v, "object")
          }
          (v, None)
        }
        case f:parser.FuncExpr => {
          // this is a function f, create a new var v and add a persistence [all] f == v;
          val varName = context.getNewLocalVar("object")
          val ts = new AbstractTemporalStatement(
            TemporalAnnotation("start","end"),
            new AbstractPersistence(AbstractParameterizedStateVariable(pb, context, f), varName))
          (varName, Some(ts))
        }
      }
    })

    val actionRefId =
      if(ar.id.nonEmpty)
        new LActRef(ar.id)
      else
        new LActRef(newLocalActionRef)

    context.addUndefinedAction(actionRefId)

    val statements = args.map(_._2).flatten

    Pair(new AbstractActionRef(ar.name, args.map(_._1), actionRefId), statements)
  }

}
