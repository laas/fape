package planstack.anml.model.concrete

import planstack.anml.model.concrete.statements.TemporalStatement
import planstack.anml.model.{AnmlProblem, VariableFactory, AbstractDecomposition, Context}
import planstack.anml.ANMLException

class Decomposition(val context:Context, val statements:List[TemporalStatement], val actions:List[Action]) extends StateModifier{

}

object Decomposition {

  def apply(pb:AnmlProblem, parent:Action, dec:AbstractDecomposition, factory:VariableFactory) : Decomposition = {
    val context = dec.context.buildContext(Some(parent.context), factory)

    val statements = dec.temporalStatements.map(TemporalStatement(context, _))

    val actions = dec.actions.map(Action(pb, _, factory, Some(parent)))

    new Decomposition(context, statements.toList, actions.toList)
  }
}
