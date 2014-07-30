package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.{Statement, TemporalStatement}
import planstack.anml.model.{AnmlProblem, Context}
import planstack.anml.ANMLException
import planstack.anml.model.abs.AbstractDecomposition
import scala.collection.mutable.ListBuffer
import java.util
import scala.collection.JavaConversions._

class Decomposition(
    val context:Context,
    val container:Action)
  extends StateModifier with TemporalInterval {


  val statements = new util.LinkedList[Statement]()
  val temporalConstraints = new util.LinkedList[TemporalConstraint]()
  val actions = new util.LinkedList[Action]()
  val actionConditions = new util.LinkedList[ActionCondition]()

  assert(context.interval == null)
  context.setInterval(this)

  def vars = seqAsJavaList(context.varsToCreate)
}


object Decomposition {

  def apply(pb:AnmlProblem, parent:Action, dec:AbstractDecomposition, actionConditions :Boolean) : Decomposition = {
    val context = dec.context.buildContext(pb, Some(parent.context))

    val decomposition = new Decomposition(context, parent)

    // annotated statements produce both statements and temporal constraints.
    val annotatedStatements =
      for(absStatement <- dec.temporalStatements) yield {
        val concrete = TemporalStatement(pb, context, absStatement)
        // update the context with a mapping from the local ID to the actual statement
        context.addStatement(absStatement.statement.id, concrete.statement)
        concrete
      }
    decomposition.statements ++= annotatedStatements.map(_.statement)
    decomposition.temporalConstraints ++= annotatedStatements.map(_.getTemporalConstraints).flatten

    if(actionConditions)
      decomposition.actionConditions ++= dec.actions.map(ActionCondition(pb, _, context, Some(parent)))
    else
      decomposition.actions ++= dec.actions.map(Action(pb, _, Some(parent), Some(context)))
    decomposition.temporalConstraints ++= dec.temporalConstraints.map(TemporalConstraint(pb, context, _))

    decomposition
  }
}
