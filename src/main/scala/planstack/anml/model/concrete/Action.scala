package planstack.anml.model.concrete

import collection.JavaConversions._

import planstack.anml.model._
import planstack.anml.model.concrete.statements.{LogStatement, TemporalStatement, Statement}
import planstack.anml.ANMLException
import planstack.anml.model.abs.{AbstractActionRef, AbstractAction}
import scala.collection.mutable.ListBuffer


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
  * @param id Global id of the action, used for future reference within anml statements.
  * @param parentAction The parent action if it is issued from a decomposition
  */
class Action(
    val abs:AbstractAction,
    val context:Context,
    val id:ActRef,
    val parentAction:Option[Action])
  extends StateModifier with TemporalInterval {

  assert(context.interval == null)
  context.setInterval(this)

  val statements = ListBuffer[TemporalStatement]()

  private var mStatus = ActionStatus.PENDING

  /** Depicts the current status of the action. It is first
    * initialized to PENDING and might be changed with `setStatus()`
    * @return
    */
  def status = mStatus

  /** Assigns a new status to the action.
    * Allowed transitions are
    *  - PENDING -> EXECUTING
    *  - EXECUTING -> (FAILED || EXECUTED)
    * @param newStatus New status of the action.
    */
  def setStatus(newStatus: ActionStatus) {
    import ActionStatus._
    mStatus match {
      case PENDING => assert(newStatus == EXECUTING)
      case EXECUTING => assert(newStatus == FAILED || newStatus == EXECUTED)
      case FAILED => throw new ANMLException("No valid status transition from FAILED.")
      case EXECUTED => throw new ANMLException("No valid status transition from EXECUTED.")
    }
    mStatus = newStatus
  }

  def vars = context.varsToCreate
  val temporalConstraints = Nil

  val container = this

  def name = abs.name
  val actions = Nil

  def decompositions = abs.decompositions

  def jDecompositions = seqAsJavaList(decompositions)

  /** Returns True if this action as possible decompositions */
  def decomposable = !decompositions.isEmpty

  /** Returns true if the statement `s` is contained in this action */
  def contains(s: LogStatement) = statements.map(_.statement).contains(s)

  /**
   * Retrieves the cost of this action.
   * Right now, it is set to 10 in all cases.
   * @return the cost of the action.
   */
  def cost = 10

  def minDuration = 5

  def maxDuration = 150

  /** Returns true if this action has a parent (ie. it is issued from a decomposition). */
  def hasParent = parentAction match {
    case Some(_) => true
    case None => false
  }

  /** Returns the parent action. Throws [[planstack.anml.ANMLException]] if this action has no parent.
    * Invocation of this method should be preceded by a call to `hasParent()` */
  def parent : Action = parentAction match {
    case Some(x) => x
    case None => throw new ANMLException("Action has no parent.")
  }

  /** Arguments (as global variables) of the action */
  def args = abs.args.map(context.getGlobalVar(_))

  /** Arguments (as global variables) of the action */
  def jArgs = seqAsJavaList(args)

  override def toString = name +"("+ abs.args.map(context.getGlobalVar(_)).mkString(", ") +")"
}


object Action {

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

    val act = new Action(abs, context, new ActRef(), parentAction)
    act.statements ++= abs.temporalStatements.map(TemporalStatement(pb, context, _)).toList

    contextOpt match {
      case Some(parent) => parent.addActionID(ref.localId, act)
      case _ =>
    }

    act
  }

  def getNewStandaloneAction(pb:AnmlProblem, actionName:String) : Action = {
    val abs =
      pb.abstractActions.find(_.name == actionName) match {
        case Some(act) => act
        case None => throw new ANMLException("Unable to find action "+actionName)
      }

    getNewStandaloneAction(pb, abs)
  }

  def getNewStandaloneAction(pb:AnmlProblem, abs:AbstractAction) : Action = {
    val parentContext = pb.context

    val context = abs.context.buildContext(pb, Some(parentContext))

    val act = new Action(abs, context, new ActRef(), None)

    act.statements ++= abs.temporalStatements.map(TemporalStatement(pb, context, _))

    act
  }
}