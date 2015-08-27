package planstack.anml.model.abs

import planstack.anml.model._
import planstack.anml.model.abs.statements.{AbstractLogStatement, AbstractResourceStatement, AbstractStatement}
import planstack.anml.model.concrete.RefCounter
import planstack.anml.{ANMLException, parser}

import scala.collection.JavaConversions._
import scala.collection.mutable

/** An abstract action is an representation for an action as it is defined in an ANML problem.
  * It gives, for an action, an abstract view of what it does, regardless of the parameters it will be given when instantiated.
  *
  * Hence all components of an action refer either to local references (such as an argument of the action) or problem
  * instances (defined in the ANML problem).
  *
  * @param taskName Name of the task this action supports
  * @param decID index of the decomposition this action was issued from. If decID == 0, then there was no decompositions.
  * @param mArgs
  * @param context
  */
class AbstractAction(val taskName:String, val decID:Int, private val mArgs:List[LVarRef], val context:PartialContext)  {

  /** True if the action was defined with the motivated keyword. False otherwise. */
  protected var motivated = false

  val name =
    if(decID == 0) "a-"+taskName
    else "m-"+taskName+decID

  /** True if the action was defined with the motivated keyword. False otherwise. */
  def mustBeMotivated = motivated

  protected var _minDur : AbstractDuration = null
  protected var _maxDur : AbstractDuration = null

  def minDur = _minDur
  def maxDur = _maxDur

  /** Arguments in the form of local references containing the name of the argument */
  def args = seqAsJavaList(mArgs)

  /** All abstract temporal statements appearing in this action */
  val statements = mutable.ArrayBuffer[AbstractStatement]()

  /** Java friendly version of [[planstack.anml.model.abs.AbstractAction#temporalStatements]]. */
  def jTemporalStatements = seqAsJavaList(statements)

  def jSubTasks = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractTask]).map(_.asInstanceOf[AbstractTask]))
  def jLogStatements = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractLogStatement]).map(_.asInstanceOf[AbstractLogStatement]))
  def jResStatements = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractResourceStatement]).map(_.asInstanceOf[AbstractResourceStatement]))
  def jTempConstraints = seqAsJavaList(statements.filter(_.isInstanceOf[AbstractTemporalConstraint]).map(_.asInstanceOf[AbstractTemporalConstraint]))

  override def toString = name
}

/** Represents a duration used to represent minimal or maximal duration f an action.
  * It can either be constant or a state variable (underlying function must be constant in time).
  *
  * @param constantDur Constant duration. SHould be set to -1 if the duration is a state variable
  * @param func State Variable representing the (parameterized) duration of the action. Should be set
  *             to null if the duration is represented by an integer.
  */
class AbstractDuration(val constantDur : Int, val func : AbstractParameterizedStateVariable) {
  require(constantDur == -1 || func == null)
  require(func == null || func.func.isConstant, "Error: a duration is represented as a non-constant function.")

  def this(constantDur : Int) = this(constantDur, null)
  def this(func : AbstractParameterizedStateVariable) = this (-1, func)

  def isConstant = func == null
}

object AbstractDuration {

  /** Creates an abstract duration from an expression. */
  def apply(e : parser.Expr, context : AbstractContext, pb : AnmlProblem) : AbstractDuration = {
    e match {
      case v : parser.NumExpr => new AbstractDuration(v.value.toInt)
      case e : parser.Expr => new AbstractDuration(StatementsFactory.asStateVariable(e, context, pb))
    }
  }
}

object AbstractAction {

  /** Factory method to build an abstract action
    *
    * @param act Action from the parser to be converted
    * @param pb Problem in which the action is defined
    * @return
    */
  def apply(act:parser.Action, pb:AnmlProblem, refCounter: RefCounter) : List[AbstractAction] = {
    val taskName = act.name
    val args = act.args.map(a => new LVarRef(a.name))

    val decompositions = act.content.filter(_.isInstanceOf[parser.Decomposition]).map(_.asInstanceOf[parser.Decomposition])
    val content = act.content.filterNot(_.isInstanceOf[parser.Decomposition])

    val decIdsAndStatements : Seq[(Int,Seq[parser.DecompositionContent])] = decompositions.size match {
      case 0 => List((0,Nil))
      case n => (1 to n).zip(decompositions.map(_.content))
    }

    val acts = for((decID, additionalStatements) <- decIdsAndStatements) yield {

      val action = new AbstractAction(taskName, decID, act.args.map(a => new LVarRef(a.name)), new PartialContext(Some(pb.context)))

      act.args foreach(arg => {
        action.context.addUndefinedVar(new LVarRef(arg.name), arg.tipe)
      })

      content foreach {
        case ts:parser.TemporalStatement => {
          action.statements ++= StatementsFactory(ts, action.context, pb, refCounter)
        }
        case tempConstraint:parser.TemporalConstraint => {
          action.statements += AbstractTemporalConstraint(tempConstraint)
        }
        case parser.Motivated =>
          action.motivated = true
        case parser.ExactDuration(e) => {
          val dur = AbstractDuration(e, action.context, pb)
          action._minDur = dur
          action._maxDur = dur
        }
        case parser.BoundedDuration(min, max) => {
          action._minDur = AbstractDuration(min, action.context, pb)
          action._maxDur = AbstractDuration(max, action.context, pb)
        }
        case const:parser.Constant => action.context.addUndefinedVar(new LVarRef(const.name), const.tipe)
        case _:parser.Decomposition => throw new ANMLException("Decomposition should have been filtered out previously")
      }

      additionalStatements foreach {
        case constraint:parser.TemporalConstraint => action.statements += AbstractTemporalConstraint(constraint)
        // constant function with no arguments is interpreted as local variable
        case const:parser.Constant => action.context.addUndefinedVar(new LVarRef(const.name), const.tipe)
        case statement:parser.TemporalStatement => {
          action.statements ++= StatementsFactory(statement, action.context, pb, refCounter)
        }
      }

      action
    }
    acts.toList
  }
}