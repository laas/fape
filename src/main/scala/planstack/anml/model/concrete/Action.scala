package planstack.anml.model.concrete

import java.util

import planstack.anml.ANMLException
import planstack.anml.model._
import planstack.anml.model.abs.{AbstractAction, AbstractActionRef}
import planstack.anml.model.concrete.statements.Statement

import scala.collection.JavaConversions._


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

  val statements = new util.LinkedList[Statement]()

  private var mStatus = ActionStatus.PENDING

  /** Depicts the current status of the action. It is first
    * initialized to PENDING and might be changed with `setStatus()`
    * @return
    */
  def status = mStatus

  /** Returns true if the action was defined with the `motivated` keyword. False otherwise. */
  def isMotivated = abs.isMotivated

  /** Assigns a new status to the action.
    * Allowed transitions are
    *  - PENDING -> EXECUTING
    *  - EXECUTING -> (FAILED || EXECUTED)
    * @param newStatus New status of the action.
    */
  def setStatus(newStatus: ActionStatus) {
    import planstack.anml.model.concrete.ActionStatus._
    mStatus match {
      case PENDING => //assert(newStatus == EXECUTING)
      case EXECUTING => //assert(newStatus == FAILED || newStatus == EXECUTED)
      case FAILED => //throw new ANMLException("No valid status transition from FAILED.")
      case EXECUTED => throw new ANMLException("No valid status transition from EXECUTED.")
    }
    mStatus = newStatus
  }

  def vars = mVars
  private val mVars = context.varsToCreate.clone()

  val temporalConstraints = new util.LinkedList[TemporalConstraint]()

  temporalConstraints += new TemporalConstraint(start, "<", end, 0)

  val container = this

  def name = abs.name
  val actions = new util.LinkedList[Action]()
  val actionConditions = new util.LinkedList[ActionCondition]()

  def decompositions = seqAsJavaList(abs.decompositions)

    /** Returns True if this action as possible decompositions */
  def decomposable = !decompositions.isEmpty

  /** Returns true if the statement `s` is contained in this action */
  def contains(s: Statement) = statements.contains(s)

  /** Tries too find the minimal duration of this action by looking at explicit constraints between the
    * start and the end time point of this action.
    * It only provides an easy way to look up the duration of an action but it might (and will) miss some implicit constraints.
    * Using this method should not prevent you from processing temporal constraints.
    * @return A lower bound on the minimal duration of the action.
    */
  def minDuration : Int = temporalConstraints.foldLeft(Integer.MIN_VALUE)((minDur, constraint) => {
    constraint match {
      case TemporalConstraint(tp1, "=", tp2, plus) if tp1 == start && tp2 == end => Math.max(minDur, - plus)
      case TemporalConstraint(tp1, "=", tp2, plus) if tp1 == end && tp2 == start => Math.max(minDur, plus)
      case TemporalConstraint(tp1, "<", tp2, plus) if tp1 == start && tp2 == end => Math.max(minDur, - plus)
      case _ => minDur
    }
  })

  /** Tries too find the maximal duration of this action by looking at explicit constraints between the
    * start and the end time point of this action.
    * It only provides an easy way to look up the duration of an action but it might (and will) miss some implicit constraints.
    * Using this method should not prevent you from processing temporal constraints.
    * @return An upper bound on the maximal duration of the action.
    */
  def maxDuration : Int = temporalConstraints.foldLeft(Integer.MAX_VALUE)((maxDur, constraint) => {
    constraint match {
      case TemporalConstraint(tp1, "=", tp2, plus) if tp1 == start && tp2 == end => Math.min(maxDur, - plus)
      case TemporalConstraint(tp1, "=", tp2, plus) if tp1 == end && tp2 == start => Math.min(maxDur, plus)
      case TemporalConstraint(tp1, "<", tp2, plus) if tp1 == end && tp2 == start => Math.min(maxDur, plus)
      case _ => maxDur
    }
  })

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
  def args = seqAsJavaList(abs.args.map(context.getGlobalVar(_)))

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
    // retrieve abstract action specified gy the action reference
    val abs =
      pb.abstractActions.find(_.name == ref.name) match {
        case Some(act) => act
        case None => throw new ANMLException("Unable to find action "+ref.name)
      }

    // map every abstract variable to the global one defined in the parent context.
    val args = ref.args.map(parentContext.getGlobalVar(_))

    newAction(pb, abs, args, ref.localId, parentAction, contextOpt)
  }

  def jNewAction(
      pb :AnmlProblem,
      abs :AbstractAction,
      args :util.List[VarRef],
      localID :LActRef,
      id : ActRef = new ActRef(),
      parentAction :Action = null,
      contextOpt :Context = null)
  : Action = {
    newAction(
      pb,
      abs,
      args,
      localID,
      if(parentAction == null) None else Some(parentAction),
      if(contextOpt == null) None else Some(contextOpt),
      id
    )
  }

  /** Builds new concrete action
    *
    * @param pb Problem in which the action appears
    * @param abs Abstract version of the action
    * @param args List of arguments (those are considered to be already existing VarRefs, if it not the case, add them to
    *             the mVars field of the resulting action.
    * @param localID Local reference to the action
    * @param parentAction Optional action in which the action to be created appears (as part of decomposition.
    * @param contextOpt Context in which the action appears, if set to None, it defaults to the problem's context.
    * @return The resulting concrete Action.
    */
  def newAction(
      pb :AnmlProblem,
      abs :AbstractAction,
      args :Seq[VarRef],
      localID :LActRef,
      parentAction :Option[Action] = None,
      contextOpt :Option[Context] = None,
      id :ActRef = new ActRef())
  : Action = {
    // containing context is the one passed, if it is empty, it defaults to the problem's
    val parentContext = contextOpt match {
      case Some(parentContext) => parentContext
      case None => pb.context
    }

    // creates pair (localVar, globalVar) as defined by the ActionRef
    val argPairs = for(i <- 0 until abs.args.length) yield (abs.args(i), args(i))
    val context = abs.context.buildContext(pb, Some(parentContext), argPairs.toMap)

    val act = new Action(abs, context, id, parentAction)

    act.addAll(abs.statements, context, pb)

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
  def getNewStandaloneAction(pb:AnmlProblem, actionName:String) : Action = {
    val abs =
      pb.abstractActions.find(_.name == actionName) match {
        case Some(act) => act
        case None => throw new ANMLException("Unable to find action "+actionName)
      }

    getNewStandaloneAction(pb, abs)
  }

  /** Creates a new Action with new VarRef as parameters.
    *
    * @param pb Problem in which the action appears
    * @param abs Abstract version of the action to create.
    * @return The new concrete action.
    */
  def getNewStandaloneAction(pb:AnmlProblem, abs:AbstractAction) : Action = {
    val act = newAction(pb, abs, abs.args.map(x => new VarRef()), new LActRef(), None, Some(pb.context))

    // for all created vars, make sure those are present in [[StateModifier#vars]]
    for(localArg <- abs.args) {
      act.mVars += ((act.context.getType(localArg), act.context.getGlobalVar(localArg)))
    }

    act
  }
}