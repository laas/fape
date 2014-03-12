package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.TemporalStatement
import planstack.anml.model.{AnmlProblem, Context}
import planstack.anml.ANMLException
import planstack.anml.model.abs.AbstractDecomposition

class Decomposition(
    val context:Context,
    val statements:List[TemporalStatement],
    val temporalConstraints:List[TemporalConstraint],
    val actions:List[Action],
    val container:Action)
  extends StateModifier with TemporalInterval {

  def vars = context.varsToCreate
}


object Decomposition {

  def apply(pb:AnmlProblem, parent:Action, dec:AbstractDecomposition) : Decomposition = {
    val context = dec.context.buildContext(pb, Some(parent.context))

    val statements = dec.temporalStatements.map(TemporalStatement(context, _))

    val actions = dec.actions.map(Action(pb, _, Some(parent), Some(context)))

    val tConst = dec.precedenceConstraints.map(TemporalConstraint(context, _))

    new Decomposition(context, statements.toList, tConst.toList, actions.toList, parent)
  }
}
