package planstack.anml.model.concrete

import planstack.anml.model._
import planstack.anml.model.concrete.statements.{TemporalStatement, Statement}
import planstack.anml.ANMLException

class Action(val abs:AbstractAction, val context:Context, val statements:List[TemporalStatement], val id:String, val parentID:Option[Action]) extends StateModifier {

  def name = abs.name
  val actions = Nil

  def decompositions = abs.decompositions


  override def toString = name +"("+ abs.args.map(context.getGlobalVar(_)).mkString(", ") +")"
}

object Action {

  private var nextActionID = 0
  private def getActionID = "action_"+{nextActionID+=1 ; nextActionID-1}


  def apply(pb:AnmlProblem, ref:AbstractActionRef, factory:VariableFactory, parentAction:Option[Action]=None) : Action = {
    val parentContext = parentAction match {
      case Some(action) => action.context
      case None => pb.context
    }
    val abs =
      pb.actions.find(_.name == ref.name) match {
        case Some(act) => act
        case None => throw new ANMLException("Unable to find action "+ref.name)
      }

    // creates pair (localVar, globalVar) as defined by the ActionRef
    val argPairs = for(i <- 0 until abs.args.length) yield (abs.args(i), parentContext.getGlobalVar(ref.args(i)))
    val context = abs.context.buildContext(Some(parentContext), factory, argPairs.toMap)
    val id = getActionID
    context.addActionID(ref.localId, id)

    val statements = abs.temporalStatements.map(TemporalStatement(context, _)).toList

    val a = new Action(abs, context, statements, id, parentAction)
    println(a)

    a
  }
}