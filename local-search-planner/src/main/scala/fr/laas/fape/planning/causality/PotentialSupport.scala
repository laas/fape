package fr.laas.fape.planning.causality

import fr.laas.fape.anml.model
import fr.laas.fape.anml.model.abs.AbstractAction
import fr.laas.fape.anml.model.LVarRef

import scala.collection.JavaConverters._
import scala.collection.mutable
import fr.laas.fape.constraints.meta.domains.{Domain, EmptyDomain}
import fr.laas.fape.constraints.meta.types.statics.Type
import fr.laas.fape.constraints.meta.util.Assertion._

class PotentialSupport(context: CausalHandler) {

  private val planner = context.context
  private val pb = planner.pb

  val actionPotentialSupports = mutable.Map[AbstractAction, ActionPotentialSupport]()

  for(a <- pb.abstractActions.asScala) {
    actionPotentialSupports.put(a, new ActionPotentialSupport(a, context))
  }

  def report : String = "--- Potential supports ---\n" +
    actionPotentialSupports.values.map(_.report).mkString("\n")

  def clone(newContext: CausalHandler) : PotentialSupport = this // should be perfectly immutable
}


class ActionPotentialSupport(val act: AbstractAction, private var context: CausalHandler) {

  val byFunction = mutable.Map[model.Function, Array[Domain]]()

  private val planner = context.context
  private val pb = planner.pb

  for(s <- act.logStatements if s.hasEffectAtEnd) {
    val funcDom = planner.func(s.sv.func).initialDomain(planner.csp)
    val argDoms = s.sv.args.map(arg => domainOf(arg))
    val valueDom = domainOf(s.effectValue)

    val domains = (funcDom :: argDoms) :+ valueDom
    if(DEBUG_LEVEL >= 1) {
      for((dom, typ) <- domains.zip(typesOf(s.sv.func)))
        assert1(dom.values.forall(typ.hasValue),
          s"In action $act, the assertion $s has at least one variable that does not match the function's types.")
    }
    add(s.sv.func, domains)
  }

  private def add(func: model.Function, doms: Seq[Domain]) {
    val arr = byFunction.getOrElseUpdate(func, Array.fill(doms.size)(new EmptyDomain))
    for(i <- arr.indices)
      arr(i) = arr(i) + doms(i)
  }

  private def domainOf(localVar: LVarRef) : Domain = {
    if(act.context.hasGlobalVar(localVar))
      planner.variable(act.context.getGlobalVar(localVar)).initialDomain(planner.csp)
    else
      planner.types.get(localVar.getType).asDomain
  }

  private def typesOf(func: model.Function) : Seq[Type[_]] = {
    planner.types.functionType :: (func.argTypes :+ func.valueType).map(planner.types.get)
  }

  def report : String = {
    val sb = new StringBuilder
    sb.append(s"Supports for action $act\n")
    var types : Any = null
    for(f <- byFunction.keys) {
      types = typesOf(f)
      println(types)
      sb.append("  ")
      sb.append(byFunction(f).zip(typesOf(f)).map{ case (dom, typ) => typ.viewOf(dom).toString}.mkString("  "))
      sb.append("\n")
    }
    sb.toString()
  }
}
