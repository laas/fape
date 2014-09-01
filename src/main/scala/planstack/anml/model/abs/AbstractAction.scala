package planstack.anml.model.abs

import planstack.anml.model.abs.statements.{AbstractResourceStatement, AbstractLogStatement, AbstractStatement}
import planstack.anml.model.{AnmlProblem, LVarRef, PartialContext}
import planstack.anml.parser

import scala.collection.JavaConversions._
import scala.collection.mutable

/** An abstract action is an representation for an action as it is defined in an ANML problem.
  * It gives, for an action, an abstract view of what it does, regardless of the parameters it will be given when instantiated.
  *
  * Hence all components of an action refer either to local references (such as an argument of the action) or problem
  * instances (defined in the ANML problem).
  *
  * @param name
  * @param mArgs
  * @param context
  */
class AbstractAction(val name:String, private val mArgs:List[LVarRef], val context:PartialContext)  {

  /** True if the action was defined with the motivated keyword. False otherwise. */
  protected var motivated = false

  /** True if the action was defined with the motivated keyword. False otherwise. */
  def isMotivated = motivated

  /** Arguments in the form of local references containing the name of the argument */
  def args = seqAsJavaList(mArgs)

  /** All abstract decompositions appearing in this action */
  val decompositions = mutable.ArrayBuffer[AbstractDecomposition]()

  /** Java-friendly version of [[planstack.anml.model.abs.AbstractAction#decompositions]]. */
  def jDecompositions = seqAsJavaList(decompositions)

  /** All abstract temporal statements appearing in this action */
  val statements = mutable.ArrayBuffer[AbstractStatement]()

  /** Java friendly version of [[planstack.anml.model.abs.AbstractAction#temporalStatements]]. */
  def jTemporalStatements = seqAsJavaList(statements)

  def jActions = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractActionRef]).map(_.asInstanceOf[AbstractActionRef]))
  def jLogStatements = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractLogStatement]).map(_.asInstanceOf[AbstractLogStatement]))
  def jResStatements = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractResourceStatement]).map(_.asInstanceOf[AbstractResourceStatement]))
  def jTempConstraints = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractTemporalConstraint]).map(_.asInstanceOf[AbstractTemporalConstraint]))

  override def toString = name
}

object AbstractAction {

  /** Factory method to build an abstract action
    *
    * @param act Action from the parser to be converted
    * @param pb Problem in which the action is defined
    * @return
    */
  def apply(act:parser.Action, pb:AnmlProblem) : AbstractAction = {
    val action = new AbstractAction(act.name, act.args.map(a => new LVarRef(a.name)), new PartialContext(Some(pb.context)))

    act.args foreach(arg => {
      action.context.addUndefinedVar(new LVarRef(arg.name), arg.tipe)
    })

    act.content foreach( _ match {
      case ts:parser.TemporalStatement => {
        action.statements ++= StatementsFactory(ts, action.context, pb)
      }
      case dec:parser.Decomposition => {
        action.decompositions += AbstractDecomposition(pb, action.context, dec)
      }
      case tempConstraint:parser.TemporalConstraint => {
        action.statements += AbstractTemporalConstraint(tempConstraint)
      }
      case parser.Motivated => action.motivated = true
    })

    action
  }
}