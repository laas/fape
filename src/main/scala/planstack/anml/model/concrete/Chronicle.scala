package planstack.anml.model.concrete

import java.util

import planstack.anml.ANMLException
import planstack.anml.model.abs.statements.{AbstractBindingConstraint, AbstractLogStatement, AbstractResourceStatement, AbstractStatement}
import planstack.anml.model.abs.{AbstractActionRef, AbstractTemporalConstraint}
import planstack.anml.model.concrete.statements.{BindingConstraint, LogStatement, ResourceStatement, Statement}
import planstack.anml.model.{AnmlProblem, Context}
import planstack.structures.IList
import planstack.structures.Converters._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/** A chronicle decribes modifications to be made to plan.
  *
  * Notable classes implementing it are [[planstack.anml.model.concrete.Action]]
  * and [[planstack.anml.model.concrete.Decomposition]].
  * Updates to a problem (such as the happening of exogeneous events) are also encoded as StateModifiers
  * in [[planstack.anml.model.AnmlProblem]].
  *
  * Components:
  *  - `vars`: global variables that need to be declared for applying the chronicle.
  *  - `statements`: individual statements depicting a condition of a change on a state variable. Those
  *    come from the effects/preconditions of actions, conditions on decomposition or exogeneous events.
  *  - `actions`: actions to be inserted in the plan. Note that actions are StateModifiers themselves.
  *
  */
trait Chronicle {

  /** A temporal interval in which the chronicle is applied. For instance, if this chronicle refers to
    * an action, the container would refer to the [start, end] interval of this action.
    * ANML temporal annotations such as [start] refer to this temporal interval.
    * Note that time points might appear outside this interval, for instance with the annotations
    * [start-10], [end+10] or [7].
    */
  def container : TemporalInterval

  /** Temporally annotated statements to be inserted in the plan */
  def statements : java.util.List[Statement]

  /** Constraints over constant functions and variables */
  def bindingConstraints : java.util.List[BindingConstraint]

  /** Returns all logical statements */
  def logStatements : java.util.List[LogStatement] = seqAsJavaList(statements.filter(_.isInstanceOf[LogStatement]).map(_.asInstanceOf[LogStatement]))

  /** Returns all logical statements */
  def resourceStatements : java.util.List[ResourceStatement] = seqAsJavaList(statements.filter(_.isInstanceOf[ResourceStatement]).map(_.asInstanceOf[ResourceStatement]))

  /** Actions to be inserted in the plan */
  def actions : java.util.List[Action]

  /** Actions conditions that must be fulfilled by the plan.
    *
    * An action condition has an action name, a set of parameters and two timepoints.
    * It can be fulfilled/supported by an action with the same whose parameters and
    * time points are equal to those of the action condition.
    */
  def actionConditions : java.util.List[ActionCondition]

  /** (Type, Reference) of global variables to be declared */
  def vars : java.util.List[Pair[String, VarRef]]

  /** All problem instances to be declared
    * Problem instances are typically a global variable with a domain containing only one value (itself).
    */
  def instances : java.util.List[String] = Nil

  protected def temporalConstraints : java.util.List[TemporalConstraint]

  def addAll(absStatements : Seq[AbstractStatement], context:Context, pb:AnmlProblem): Unit = {
    for(absStatement <- absStatements) {
      absStatement match {
        case s: AbstractLogStatement =>
          val binded = s.bind(context, pb, this)
          statements += binded
          context.addStatement(s.id, binded)

        case s: AbstractResourceStatement =>
          val binded = s.bind(context, pb, this)
          statements += binded
          context.addStatement(s.id, binded)

        case s:AbstractTemporalConstraint =>
          temporalConstraints += s.bind(context, pb, this)

        case s:AbstractActionRef =>
          val parent = this match {
              case x: Action => Some(x)
              case decomposition: Decomposition => Some(decomposition.container)
              case _ => None
            }
          if (pb.usesActionConditions)
            actionConditions += ActionCondition(pb, s, context, parent)
          else
            actions += Action(pb, s, parent, Some(context))

        case s:AbstractBindingConstraint =>
          bindingConstraints += s.bind(context, pb, this)
          
        case _ => throw new ANMLException("unsupported yet:" + absStatement)
      }
    }
  }

  /** Builds an object containing all temporal operations (time point creations and constraints) that must be applied
    * by this chronicle
    */
  def getTemporalObjects: TemporalObjects = {
    // we can make virtual the time points of actions and those of the container
    val fixedTPs = (
      for(interval:TemporalInterval <- container :: actions.asScala.toList) yield
        List(interval.start,interval.end)
      ).flatten.toSet


    val equalities = temporalConstraints.filter(_.op == "=")
    val rigidsRawConstraints =
      equalities
        .filter(tc => fixedTPs.contains(tc.tp1) || fixedTPs.contains(tc.tp2)) // at least one must be fixed
        .filter(tc => !fixedTPs.contains(tc.tp1) || !fixedTPs.contains(tc.tp2)) // at least one must not be fixed

    // a list of (virt, real, dist(virt, real))
    val rigids = rigidsRawConstraints.map(tc => {
      if (fixedTPs contains tc.tp2)
        (tc.tp1, tc.tp2, -tc.plus :Integer)
      else
        (tc.tp2, tc.tp1, tc.plus :Integer)
    }).asScala

    val virtualTimePoints = rigids.map(_._1)

    val tps = new ListBuffer[(TPRef, String)]
    val pendingVirtuals = new ListBuffer[TPRef]

    this match {
      case _:Action => {
        tps.append((container.start, "dispatchable"))
        tps.append((container.end, "contingent"))
      }
      case _ => //start/end are those of the containing action or of the problem
    }

    // statements and action conditions are regular time point (if they are not virtual)
    for(s <- statements ++ actionConditions) {
      if(!virtualTimePoints.contains(s.start)) tps.append((s.start, "controllable"))
      if(!virtualTimePoints.contains(s.end)) tps.append((s.end, "controllable"))
    }
//    Let action conditions be regular time points for now
//    for(tc <- actionConditions) {
//      pendingVirtuals.append(tc.start, tc.end)
//    }


    val nonRigidConstraints = temporalConstraints.filter(c => !rigidsRawConstraints.contains(c))

    new TemporalObjects(tps.toList, rigids.toList, pendingVirtuals, nonRigidConstraints)
  }
}

/**
 * This object sums all time points and constraints in a state modifier.
 * Note that some constraints might apply on time points not defined here
 * (e.g. start of a nested action or global start of the problem).
 *
 * @param timePoints Non virtual time points that chould be recorded.
 * @param virtualTimePoints Virtual time points, those are defined wrt to another time point. It comes as a tuple
 *                          (virt, real, d) where real is a non-virtual timepoint d is the distance from virt to real
 * @param pendingVirtuals Timepoints that will be tied later on to another timepoint.
 * @param nonRigidConstraints All constraints that are not represented as a virtual time point and its rigid relations.
 */
class TemporalObjects(val timePoints: IList[Pair[TPRef, String]],
                      val virtualTimePoints: IList[(TPRef, TPRef, Integer)],
                      val pendingVirtuals: IList[TPRef],
                      val nonRigidConstraints: IList[TemporalConstraint])

class BaseChronicle(val container: TemporalInterval) extends Chronicle {

  val statements = new util.LinkedList[Statement]()
  val bindingConstraints = new util.LinkedList[BindingConstraint]()
  val actions = new util.LinkedList[Action]()
  val actionConditions = new util.LinkedList[ActionCondition]()
  val vars = new util.LinkedList[Pair[String, VarRef]]()
  override val instances = new util.LinkedList[String]()
  val temporalConstraints = new util.LinkedList[TemporalConstraint]()
}