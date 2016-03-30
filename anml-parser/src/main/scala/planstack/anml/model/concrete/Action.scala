package planstack.anml.model.concrete

import java.util

import planstack.anml.ANMLException
import planstack.anml.model._
import planstack.anml.model.abs.{AbstractAction, AbstractDuration, AbstractTask}
import planstack.anml.model.concrete.statements.Statement
import planstack.anml.model.concrete.time.TimepointRef

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
    val parentAction:Option[Action],
    refCounter: RefCounter)
  extends Chronicle with TemporalInterval {

  assert(context.interval == null)
  context.setInterval(this)

  override val start : TPRef = new TPRef(refCounter)
  override val end : TPRef = new TPRef(refCounter)

  val statements = new util.LinkedList[Statement]()

  val instantiationVar : VarRef = new VarRef("integer", refCounter)

  val bindingConstraints = new util.LinkedList[BindingConstraint]()

  private var mStatus = ActionStatus.PENDING

  /** Depicts the current status of the action. It is first
    * initialized to PENDING and might be changed with `setStatus()`
    * @return
    */
  def status = mStatus

  /** Returns true if the action was defined with the `motivated` keyword. False otherwise. */
  def mustBeMotivated = abs.mustBeMotivated

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
      case EXECUTED => //throw new ANMLException("No valid status transition from EXECUTED.")
    }
    mStatus = newStatus
  }

  def vars = mVars
  private val mVars = context.varsToCreate.clone()

  val temporalConstraints = new util.LinkedList[TemporalConstraint]()

  val container = this

  def taskName = abs.taskName
  def name = abs.name
  val actions = new util.LinkedList[Action]()
  val tasks = new util.LinkedList[Task]()

  /** Returns true if the statement `s` is contained in this action */
  def contains(s: Statement) = statements.contains(s)

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
  lazy val args = seqAsJavaList(abs.args.map(context.getGlobalVar(_)))

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
  def apply(pb:AnmlProblem, ref:AbstractTask, refCounter: RefCounter, parentAction:Option[Action]=None, contextOpt:Option[Context]=None) : Action = {
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
    assert(abs.args.size() == ref.args.size, "Error: wrong number of arguments in action ref: "+ref)

    // map every abstract variable to the global one defined in the parent context.
    val args = ref.args.map(parentContext.getGlobalVar(_))

    newAction(pb, abs, args, ref.localId, refCounter, parentAction, contextOpt)
  }

  def jNewAction(
      pb :AnmlProblem,
      abs :AbstractAction,
      args :util.List[VarRef],
      localID :LActRef,
      refCounter: RefCounter,
//      id : ActRef = new ActRef(),
      parentAction :Action = null,
      contextOpt :Context = null)
  : Action = {
    newAction(
      pb,
      abs,
      args,
      localID,
      refCounter,
      if(parentAction == null) None else Some(parentAction),
      if(contextOpt == null) None else Some(contextOpt)
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
      refCounter: RefCounter,
      parentAction :Option[Action] = None,
      contextOpt :Option[Context] = None) : Action =
  {
    // containing context is the one passed, if it is empty, it defaults to the problem's
    val parentContext = contextOpt match {
      case Some(parentContext) => parentContext
      case None => pb.context
    }
    assert(abs.args.size() == args.size, "Wrong number of arguments ("+args.size+") for action: "+abs)

    // creates pair (localVar, globalVar) as defined by the ActionRef
    val argPairs = for(i <- 0 until abs.args.length) yield (abs.args(i), args(i))
    val context = abs.context.buildContext(pb, Some(parentContext), refCounter, argPairs.toMap)
    val id = new ActRef(refCounter)
    val act = new Action(abs, context, id, parentAction, refCounter)

    val ctgTimepoints = act.temporalConstraints
      .filter(tc => tc.isInstanceOf[ContingentConstraint])
      .map(tc => tc.dst)
      .toSet

    act.addAllStatements(abs.statements, context, pb, refCounter)
    act.addAllConstraints(abs.constraints, context, pb, refCounter)

    act.flexibleTimepoints = abs.flexibleTimepoints
      .map(tp => TimepointRef(pb, context, tp, refCounter))
    assert(act.flexibleTimepoints.asScala.toSet.size == act.flexibleTimepoints.size)

    act.flexibleTimepoints.foreach(tp =>
      if(tp == act.start) tp.setDispatchable()
      else if(ctgTimepoints.contains(tp)) tp.setContingent()
      else tp.setStructural()
    )

    act.anchoredTimepoints = abs.anchoredTimepoints
      .map(at => new AnchoredTimepoint(TimepointRef(pb, context, at.timepoint, refCounter), TimepointRef(pb, context, at.anchor, refCounter), at.delay))

    act.anchoredTimepoints.foreach(at => at.timepoint.setVirtual())

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

    val act = newAction(pb, abs, abs.args.map(x => new VarRef(abs.context.getType(x), refCounter)), new LActRef(), refCounter, None, Some(pb.context))

    // for all created vars, make sure those are present in [[StateModifier#vars]]
    for(localArg <- abs.args) {
      act.mVars += act.context.getGlobalVar(localArg)
    }

    act
  }
}