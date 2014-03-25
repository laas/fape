package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.{Statement, TemporalStatement}
import planstack.anml.model.{AnmlProblem, Context}
import planstack.anml.ANMLException
import planstack.anml.model.abs.AbstractDecomposition
import scala.collection.mutable.ListBuffer

class Decomposition(
    val context:Context,
    val container:Action)
  extends StateModifier with TemporalInterval {


  val statements = ListBuffer[Statement]()
  val temporalConstraints = ListBuffer[TemporalConstraint]()
  val actions = ListBuffer[Action]()

  assert(context.interval == null)
  context.setInterval(this)

  def vars = context.varsToCreate
}


object Decomposition {

  def apply(pb:AnmlProblem, parent:Action, dec:AbstractDecomposition) : Decomposition = {
    val context = dec.context.buildContext(pb, Some(parent.context))

    val decomposition = new Decomposition(context, parent)

    // the annotated statements produce both statements and temporal constraints
    val annotatedStatements = dec.temporalStatements.map(TemporalStatement(pb, context, _))
    decomposition.statements ++= annotatedStatements.map(_.statement)
    decomposition.temporalConstraints ++= annotatedStatements.map(_.getTemporalConstraints).flatten

    decomposition.actions ++= dec.actions.map(Action(pb, _, Some(parent), Some(context)))
    decomposition.temporalConstraints ++= dec.precedenceConstraints.map(TemporalConstraint(pb, context, _))

    decomposition
  }
}
