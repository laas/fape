package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.statements.AbstractStatement
import planstack.anml.model.concrete.{RefCounter, Chronicle}

import scala.collection.JavaConverters._

/** Reference to an action as it appears in a decomposition
  *
  * @param name Name of the action
  * @param args Parameters of the action as instances of local variables
  * @param localId Local reference to the action.
  */
class AbstractActionRef(val name:String, val args:List[LVarRef], val localId:LActRef) extends AbstractStatement(localId) {
  require(localId nonEmpty)
  require(name nonEmpty, s"Emptyname: args: ${args.mkString(", ")}  localid: $localId")

  def jArgs : java.util.List[LVarRef] = args.asJava

  override def bind(context:Context, pb:AnmlProblem, container: Chronicle, refCounter: RefCounter) = throw new UnsupportedOperationException

  override def isTemporalInterval = true

  override def toString = name+"("+args.mkString(",")+")"
}

