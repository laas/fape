package planstack.anml.model.abs

import planstack.anml.{ANMLException, parser}
import scala.collection.mutable
import planstack.anml.parser._
import planstack.anml.parser.TemporalStatement
import planstack.anml.parser.ActionRef
import planstack.anml.model.{PartialContext, AnmlProblem}


class AbstractAction(val name:String, val args:List[String], val context:PartialContext)  {

  val decompositions = mutable.ArrayBuffer[AbstractDecomposition]()
  val temporalStatements = mutable.ArrayBuffer[AbstractTemporalStatement]()

}

object AbstractAction {

  def apply(act:parser.Action, pb:AnmlProblem) : AbstractAction = {
    val action = new AbstractAction(act.name, act.args.map(_.name), new PartialContext(Some(pb.context)))

    act.args foreach(arg => {
      action.context.addUndefinedVar(arg.name, arg.tipe)
    })

    act.content foreach( _ match {
      case ts:parser.TemporalStatement => {
        action.temporalStatements += AbstractTemporalStatement(pb, action.context, ts)
      }
      case dec:parser.Decomposition => {
        action.decompositions += AbstractDecomposition(pb, action.context, dec)
      }
    })

    action
  }
}