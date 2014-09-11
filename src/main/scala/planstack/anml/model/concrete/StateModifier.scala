package planstack.anml.model.concrete

import java.util

import planstack.anml.ANMLException
import planstack.anml.model.abs.statements.{AbstractBindingConstraint, AbstractLogStatement, AbstractResourceStatement, AbstractStatement}
import planstack.anml.model.abs.{AbstractActionRef, AbstractTemporalConstraint}
import planstack.anml.model.concrete.statements.{BindingConstraint, LogStatement, ResourceStatement, Statement}
import planstack.anml.model.{AnmlProblem, Context}

import scala.collection.JavaConversions._

/** A state modifier decribes modifications to be made to plan.
  *
  * Notable classes implementing it are [[planstack.anml.model.concrete.Action]]
  * and [[planstack.anml.model.concrete.Decomposition]].
  * Updates to a problem (such as the happening of exogeneous events) are also encoded as StateModifiers
  * in [[planstack.anml.model.AnmlProblem]].
  *
  * Components:
  *  - `vars`: global variables that need to be declared for applying the modifier.
  *  - `statements`: individual statements depicting a condition of a change on a state variable. Those
  *    come from the effects/preconditions of actions, conditions on decomposition or exogeneous events.
  *  - `actions`: actions to be inserted in the plan. Note that actions are StateModifiers themselves.
  *
  */
trait StateModifier {

  /** A temporal interval in which the modifier is applied. For instance, if this StateModifier refers to
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

  def temporalConstraints : java.util.List[TemporalConstraint]

  def addAll(absStatements : Seq[AbstractStatement], context:Context, pb:AnmlProblem): Unit = {
    for(absStatement <- absStatements) {
      absStatement match {
        case s: AbstractLogStatement => {
          val binded = s.bind(context, pb)
          statements += binded
          context.addStatement(s.id, binded)
        }
        case s: AbstractResourceStatement => {
          val binded = s.bind(context, pb)
          statements += binded
          context.addStatement(s.id, binded)
        }
        case s:AbstractTemporalConstraint =>
          temporalConstraints += s.bind(context, pb)
        case s:AbstractActionRef => {
          val parent =
            if (this.isInstanceOf[Action]) Some(this.asInstanceOf[Action])
            else None
          if (pb.usesActionConditions) {
            actionConditions += ActionCondition(pb, s, context, parent)
          } else {
            actions += Action(pb, s, parent, Some(context))
          }
        }
        case s:AbstractBindingConstraint =>
          bindingConstraints += s.bind(context, pb)
        case _ => throw new ANMLException("unsupported yet:" + absStatement)
      }
    }
  }
}

class BaseStateModifier(val container: TemporalInterval) extends StateModifier {

  val statements = new util.LinkedList[Statement]()
  val bindingConstraints = new util.LinkedList[BindingConstraint]()
  val actions = new util.LinkedList[Action]()
  val actionConditions = new util.LinkedList[ActionCondition]()
  val vars = new util.LinkedList[Pair[String, VarRef]]()
  override val instances = new util.LinkedList[String]()
  val temporalConstraints = new util.LinkedList[TemporalConstraint]()
}