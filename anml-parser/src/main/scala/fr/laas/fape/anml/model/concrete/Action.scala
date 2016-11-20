package fr.laas.fape.anml.model.concrete

import fr.laas.fape.anml.ANMLException
import fr.laas.fape.anml.model.abs.AbstractAction
import fr.laas.fape.anml.model.{Context, _}
import fr.laas.fape.anml.model.concrete.statements.Statement

import scala.collection.JavaConversions._


/** Represents a concrete action that is to be inserted into a plan. All parameters of the action refer to one global
  * variable.
  *
  * An action implements StateModifier by exposing all TemporalStatement that should be inserted in the plan after
  * applying the action.
  * An action implements TemporalInterval which gives it two timepoints start and end that respectively map
  * to the start time and end time of the action.
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
    val interval: TemporalInterval,
    val chronicle: Chronicle,
    val parentAction:Option[Action],
    refCounter: RefCounter)
  extends TemporalInterval with ChronicleContainer with VariableUser {

  chronicle.container = Some(this)

  def label = context.label

  override val start : TPRef = interval.start
  override val end : TPRef = interval.end

  def statements = chronicle.statements

  val instantiationVar : VarRef = new VarRef(TInteger, refCounter, Label(label,"instantiation_var"))

  def bindingConstraints = chronicle.bindingConstraints

  private var mStatus = ActionStatus.PENDING

  /** Depicts the current status of the action. It is first
    * initialized to PENDING and might be changed with `setStatus()`
 *
    * @return
    */
  def status = mStatus

  /** Returns true if the action was defined with the `motivated` keyword. False otherwise. */
  def mustBeMotivated = abs.isTaskDependent

  /** Assigns a new status to the action.
    * Allowed transitions are
    *  - PENDING -> EXECUTING
    *  - EXECUTING -> (FAILED || EXECUTED)
 *
    * @param newStatus New status of the action.
    */
  def setStatus(newStatus: ActionStatus) {
    import ActionStatus._
    mStatus match {
      case PENDING => //assert(newStatus == EXECUTING)
      case EXECUTING => //assert(newStatus == FAILED || newStatus == EXECUTED)
      case FAILED => //throw new ANMLException("No valid status transition from FAILED.")
      case EXECUTED => //throw new ANMLException("No valid status transition from EXECUTED.")
    }
    mStatus = newStatus
  }

  def vars = chronicle.vars

  def temporalConstraints = chronicle.temporalConstraints

  val container = this

  def taskName = abs.taskName
  def name = abs.name
  def tasks = chronicle.tasks
  def logStatements = chronicle.logStatements

  /** Returns true if the statement `s` is contained in this action */
  def contains(s: Statement) = statements.contains(s)

  /** Returns true if this action has a parent (ie. it is issued from a decomposition). */
  def hasParent = parentAction match {
    case Some(_) => true
    case None => false
  }

  /** Returns the parent action. Throws [[ANMLException]] if this action has no parent.
    * Invocation of this method should be preceded by a call to `hasParent()` */
  def parent : Action = parentAction match {
    case Some(x) => x
    case None => throw new ANMLException("Action has no parent.")
  }

  /** Arguments (as global variables) of the action */
  lazy val args = seqAsJavaList(abs.args.map(context.getGlobalVar(_)))

  override def toString = name +"("+ abs.args.map(context.getGlobalVar(_)).mkString(", ") + ")"

  override def usedVariables = chronicle.usedVariables ++ args + start + end + instantiationVar
}


object Action {


  /** Builds new concrete action
    *
    * @param pb Problem in which the action appears
    * @param abs Abstract version of the action
    * @param localID Local reference to the action
    * @param parentAction Optional action in which the action to be created appears (as part of decomposition.
    * @param contextOpt Context in which the action appears, if set to None, it defaults to the problem's context.
    * @return The resulting concrete Action.
    */
  def newAction(
      pb :AnmlProblem,
      abs :AbstractAction,
      localID :LActRef,
      refCounter: RefCounter,
      parentAction :Option[Action] = None,
      contextOpt :Option[Context] = None) : Action =
  {
    // containing context is the one passed, if it is empty, it defaults to the problem's
    val parentContext = contextOpt match {
      case Some(parentContext) => parentContext
      case None => pb.context
    }

    // creates pair (localVar, globalVar) as defined by the ActionRef
    val id = new ActRef(refCounter)
    val interval = new TemporalInterval {
      override val start: TPRef = new TPRef(refCounter)
      override val end: TPRef = new TPRef(refCounter)
    }
    val context = abs.context.buildContext(pb, "act"+id.id, Some(parentContext), refCounter, interval)
    val chronicle = abs.chron.getInstance(context, interval, pb, refCounter)
    val act = new Action(abs, context, id, interval, chronicle, parentAction, refCounter)

    // if there is a parent action, add a mapping localId -> globalID to its context
    contextOpt match {
      case Some(parent) => parent.addAction(localID, act)
      case _ =>
    }

    act
  }

  /** Creates a new Action with new VarRef as parameters.
    *
    * @param pb Problem in which the action appears
    * @param actionName Name of the action to create.
    * @return The new concrete action.
    */
  def getNewStandaloneAction(pb:AnmlProblem, actionName:String, refCounter: RefCounter) : Action = {
    val abs =
      pb.abstractActions.find(_.name == actionName) match {
        case Some(act) => act
        case None => throw new ANMLException("Unable to find action "+actionName)
      }

    getNewStandaloneAction(pb, abs, refCounter)
  }

  /** Creates a new Action with new VarRef as parameters.
    *
    * @param pb Problem in which the action appears
    * @param abs Abstract version of the action to create.
    * @return The new concrete action.
    */
  def getNewStandaloneAction(pb:AnmlProblem, abs:AbstractAction, refCounter: RefCounter) : Action = {

    val act = newAction(pb, abs, new LActRef(), refCounter, None, Some(pb.context)) //TODO: fix with real context

    act
  }
}