package planstack.anml.model.concrete

import planstack.anml.model._
import planstack.anml.model.concrete.statements.{TemporalStatement, Statement}
import planstack.anml.ANMLException
import planstack.anml.model.abs.{AbstractActionRef, AbstractAction}


/** Represents a concrete action that is to be inserted into a plan. All parameters of the action refer to one global
  * variable.
  *
  * An action implements StateModifier by exposing all TemporalStatement that should be inserted in the plan after
  * applying the action.
  * An action implements TemporalInterval which gives it two timepoints start and end that respectively map
  * to the start time and end time of the action.
  *
  *
  * @param abs The AbstractAction of which this Action is an instance
  * @param context Context of the action, containing mapping from local to global variables. Most notably contains a
  *                mapping from parameters to actual global variables. It differs from
  *                the context in which the action is declared (that can be accessed in `context.parentContext`.
  * @param statements Concrete statements to be inserted in the plan when this action is applied
  * @param id Global id of the action, used for future reference within anml statements.
  * @param parentAction The parent action if it is issued from a decomposition
  */
class Action(
    val abs:AbstractAction,
    val context:Context,
    val statements:List[TemporalStatement],
    val id:String,
    val parentAction:Option[Action])
  extends StateModifier with TemporalInterval {

  def vars = context.varsToCreate

  def name = abs.name
  val actions = Nil

  def decompositions = abs.decompositions

  override def toString = name +"("+ abs.args.map(context.getGlobalVar(_)).mkString(", ") +")"
}


object Action {

  private var nextActionID = 0
  protected def getActionID = "action_"+{nextActionID+=1 ; nextActionID-1}


  /**
   * Creates a new action from an action reference.
   *
   * @param pb Problem in which the action appears
   * @param ref Reference to the action (name, arguments, ...)
   * @param parentAction Option: Action in which the reference appears if it derives from a decomposition
   * @param contextOpt Option: Context of the decomposition
   * @return concrete action as a StateModifier
   */
  def apply(pb:AnmlProblem, ref:AbstractActionRef, parentAction:Option[Action]=None, contextOpt:Option[Context]=None) : Action = {
    val parentContext = contextOpt match {
      case Some(parentContext) => parentContext
      case None => pb.context
    }
    val abs =
      pb.abstractActions.find(_.name == ref.name) match {
        case Some(act) => act
        case None => throw new ANMLException("Unable to find action "+ref.name)
      }

    // creates pair (localVar, globalVar) as defined by the ActionRef
    val argPairs = for(i <- 0 until abs.args.length) yield (abs.args(i), parentContext.getGlobalVar(ref.args(i)))
    val context = abs.context.buildContext(pb, Some(parentContext), argPairs.toMap)
    val id = getActionID
    context.addActionID(ref.localId, id)

    val statements = abs.temporalStatements.map(TemporalStatement(context, _)).toList

    new Action(abs, context, statements, id, parentAction)
  }

  def getNewRootAction(pb:AnmlProblem, actionName:String) : Action = {
    val parentContext = pb.context

    val abs =
      pb.abstractActions.find(_.name == actionName) match {
        case Some(act) => act
        case None => throw new ANMLException("Unable to find action "+actionName)
      }

    val context = abs.context.buildContext(pb, Some(parentContext))
    val id = getActionID
    context.addActionID("", id)

    val statements = abs.temporalStatements.map(TemporalStatement(context, _)).toList

    new Action(abs, context, statements, id, None)

  }
}