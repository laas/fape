package planstack.anml.model.concrete

import java.util

import planstack.anml.model.abs.AbstractDecomposition
import planstack.anml.model.concrete.statements.{BindingConstraint, Statement}
import planstack.anml.model.{AnmlProblem, Context}

import scala.collection.JavaConversions._

class Decomposition(
    val context:Context,
    val container:Action)
  extends StateModifier with TemporalInterval {


  val statements = new util.LinkedList[Statement]()
  val bindingConstraints = new util.LinkedList[BindingConstraint]()
  val temporalConstraints = new util.LinkedList[TemporalConstraint]()
  val actions = new util.LinkedList[Action]()
  val actionConditions = new util.LinkedList[ActionCondition]()

  assert(context.interval == null)
  context.setInterval(this)

  def vars = seqAsJavaList(context.varsToCreate)
}


object Decomposition {

  def apply(pb:AnmlProblem, parent:Action, dec:AbstractDecomposition) : Decomposition = {
    val context = dec.context.buildContext(pb, Some(parent.context))

    val decomposition = new Decomposition(context, parent)

    decomposition.addAll(dec.statements, context, pb)


    decomposition
  }
}
