package fr.laas.fape.anml.model.concrete

import java.util

import fr.laas.fape.anml.model.{AnmlProblem, ChronicleContainer, Context, concrete}
import fr.laas.fape.anml.model.abs.statements.{AbstractLogStatement, AbstractResourceStatement, AbstractStatement}
import fr.laas.fape.anml.model.abs._
import fr.laas.fape.anml.model.concrete.statements._
import fr.laas.fape.anml.{ANMLException}

import scala.collection.JavaConverters._

/** A chronicle describes modifications to be made to plan.
  *
  * Notable classes containing it are [[concrete.Action]] and [[AnmlProblem]]
  * Updates to a problem (such as the happening of exogeneous events) are also encoded as Chronicle
  * in [[AnmlProblem]].
  *
  * Components:
  *  - `vars`: global variables that need to be declared for applying the chronicle.
  *  - `statements`: individual statements depicting a condition of a change on a state variable. Those
  *    come from the effects/preconditions of actions, conditions on decomposition or exogeneous events.
  *  - `actions`: actions to be inserted in the plan. Note that actions are StateModifiers themselves.
  *
  */
class Chronicle extends VariableUser {

  var container : Option[ChronicleContainer] = None

  def getLabel = container match {
    case Some(c) => c.label
    case None => "unattached-chronicle-" + System.identityHashCode(this)
  }

  /** Temporally annotated statements to be inserted in the plan */
  val statements = new util.LinkedList[Statement]()

  /** Constraints over constant functions and variables */
  val bindingConstraints = new util.LinkedList[BindingConstraint]()

  /** Returns all logical statements */
  def logStatements : java.util.List[LogStatement] = statements.asScala.collect{ case x: LogStatement => x }.asJava

  /** Returns all logical statements */
  def resourceStatements : java.util.List[ResourceStatement] = statements.asScala.collect{ case x: ResourceStatement => x }.asJava

  /** Actions conditions that must be fulfilled by the plan.
    *
    * An action condition has an action name, a set of parameters and two timepoints.
    * It can be fulfilled/supported by an action with the same whose parameters and
    * time points are equal to those of the action condition.
    */
  val tasks = new util.LinkedList[Task]()

  /** Global variables to be declared */
  val vars = new util.LinkedList[VarRef]()

  /** All problem instances to be declared
    * Problem instances are typically a global variable with a domain containing only one value (itself).
    */
  val instances = new util.LinkedList[String]()

  val temporalConstraints = new util.LinkedList[TemporalConstraint]()

  val annotations : util.List[ChronicleAnnotation] = new util.LinkedList[ChronicleAnnotation]()

  def isEmpty = statements.isEmpty && tasks.isEmpty && bindingConstraints.isEmpty && vars.isEmpty && instances.isEmpty && annotations.isEmpty

  def addStatement(statement: Statement): Unit = {
    statements.add(statement)
  }

  def addTask(task: Task) { tasks.add(task) }

  def addConstraint(constraint: Constraint): Unit = {
    constraint match {
      case c: BindingConstraint => bindingConstraints.add(c)
      case c: TemporalConstraint => temporalConstraints.add(c)
    }
  }

  def addAllStatements(absStatements : Seq[AbstractStatement], context:Context, pb:AnmlProblem, refCounter: RefCounter): Unit = {
    for(absStatement <- absStatements) {
      absStatement match {
        case s: AbstractLogStatement =>
          val binded = s.bind(context, pb, this, refCounter)
          statements.add(binded)
          context.addStatement(s.id, binded)

        case s: AbstractResourceStatement =>
          val binded = s.bind(context, pb, this, refCounter)
          statements.add(binded)
          context.addStatement(s.id, binded)

        case s:AbstractTask =>
          val parent =
            this.container match {
              case Some(x: Action) => Some(x)
              case _ => None
            }
          tasks.add(Task(pb, s, context, parent, refCounter))

        case _ => throw new ANMLException("unsupported yet:" + absStatement)
      }
    }
  }

  def addAllConstraints(absConstraints : Seq[AbstractConstraint], context:Context, pb:AnmlProblem, refCounter: RefCounter): Unit = {
    for(absConstraint <- absConstraints) {
      absConstraint match {
        case s:AbstractTemporalConstraint =>
          temporalConstraints.addAll(s.bind(context, pb, refCounter).asJava)

        case s:AbstractBindingConstraint =>
          bindingConstraints.addAll(s.bind(context, pb, refCounter).asJava)

        case _ => throw new ANMLException("unsupported yet:" + absConstraint)
      }
    }
  }

  override def usedVariables =
    (tasks.asScala.map(_.asInstanceOf[VariableUser])
      ++ temporalConstraints.asScala.map(_.asInstanceOf[VariableUser])
      ++ bindingConstraints.asScala.map(_.asInstanceOf[VariableUser])
      ++ statements.asScala.map(_.asInstanceOf[VariableUser])
      ).flatMap(_.usedVariables)
      .toSet ++ vars.asScala
}

