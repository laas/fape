package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.statements.AbstractStatement

/** Reference to an action as it appears in a dcomposition
  *
  * @param name Name of the action
  * @param args Parameters of the action as instances of local variables
  * @param localId Local reference to the action.
  */
class AbstractActionRef(val name:String, val args:List[LVarRef], val localId:LActRef) extends AbstractStatement(localId) {
  require(localId nonEmpty)
  require(name nonEmpty)

  override def bind(context:Context, pb:AnmlProblem) = throw new UnsupportedOperationException
}

