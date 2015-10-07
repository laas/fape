package planstack.anml.model.concrete

import planstack.anml.ANMLException
import planstack.anml.model.abs.{AbstractAction, AbstractTask}
import planstack.anml.model.{AnmlProblem, Context}

import scala.collection.JavaConversions._

/** A task states that some action with the same name, parameters and time points must
  * be present in the plan.
  *
  * An action condition has an action name, a set of parameters and two timepoints.
  * It can be fulfilled/supported by an action with the same whose parameters and
  * time points are equal to those of the action condition.
  */
class Task(val name: String, val args :java.util.List[VarRef], val parent:Option[Action], refCounter: RefCounter) extends TemporalInterval {

  override val start : TPRef = new TPRef(refCounter)
  override val end : TPRef = new TPRef(refCounter)

  val groundSupportersVar = new VarRef(refCounter)
  val methodSupportersVar = new VarRef(refCounter)

  override def toString = name+args.toString
}


object Task {

  def apply(pb :AnmlProblem, ref :AbstractTask, context :Context, parentActionOpt :Option[Action], refCounter: RefCounter) : Task = {
    val args = seqAsJavaList(ref.args.map(context.getGlobalVar(_)))
    val ac = new Task(ref.name, args, parentActionOpt, refCounter)
    context.addActionCondition(ref.localId, ac)
    ac
  }
}