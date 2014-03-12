package planstack.anml.model.abs

import planstack.anml.parser
import planstack.anml.model._

class AbstractActionRef(val name:String, val args:List[String], val localId:String) {
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
    val args : List[Pair[String, Option[AbstractTemporalStatement]]] = ar.args map(argExpr => {
      argExpr match {
        case v:parser.VarExpr => {
          // argument is a variable
          if(!context.contains(v.variable)) {
            // var doesn't exists, add it to context
            context.addUndefinedVar(v.variable, "object")
          }
          (v.variable, None)
        }
        case f:parser.FuncExpr => {
          // this is a function f, create a new var v and add a persistence [all] f == v;
          val varName = context.getNewLocalVar("object")
          val ts = new AbstractTemporalStatement(
            TemporalAnnotation("start","end"),
            new AbstractPersistence(ParameterizedStateVariable(pb, context, f), varName))
          (varName, Some(ts))
        }
      }
    })

    val actionRefId =
      if(ar.id.nonEmpty)
        ar.id
      else
        newLocalActionRef

    context.addUndefinedAction(actionRefId)

    val statements = args.map(_._2).flatten

    Pair(new AbstractActionRef(ar.name, args.map(_._1), actionRefId), statements)
  }

}
