package fr.laas.fape.planning.causality

import fr.laas.fape.anml.model
import fr.laas.fape.anml.model.abs.AbstractAction
import fr.laas.fape.anml.model.LVarRef
import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{ConjunctionConstraint, Constraint, ConstraintSatisfaction, Contradiction}

import scala.collection.JavaConverters._
import scala.collection.mutable
import fr.laas.fape.constraints.meta.domains.{Domain, EmptyDomain}
import fr.laas.fape.constraints.meta.events.Event
import fr.laas.fape.constraints.meta.types.statics.Type
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.meta.variables.{IVar, IntVar, IntVariable, VarWithDomain}
import fr.laas.fape.planning.structures.Holds

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

  /** Constraint require that the given variable take a value in the given domain. */
  class InDomain(variable: IntVariable, domain: Domain) extends Constraint {
    override def variables(implicit csp: CSP): Set[IVar] = Set(variable)
    override def satisfaction(implicit csp: CSP): Satisfaction =
      if(variable.domain.emptyIntersection(domain)) ConstraintSatisfaction.VIOLATED
      else if(variable.domain.containedBy(domain)) ConstraintSatisfaction.SATISFIED
      else ConstraintSatisfaction.UNDEFINED

    override protected def _propagate(event: Event)(implicit csp: CSP): Unit =
      if(variable.domain.emptyIntersection(domain))
        throw new InconsistentBindingConstraintNetwork()
      else if(variable.domain.containedBy(domain))
        csp.updateDomain(variable, variable.domain & domain)

    /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
    override def reverse: Constraint = ???
  }

  /** Constraint that requires the N given variables to take values in the N given domains. */
  class PotentialSupportFeasibility(vars: Seq[IntVariable], domains: Seq[Domain])
    extends ConjunctionConstraint(vars.zip(domains).map(x => new InDomain(x._1, x._2))) {
    require(vars.size == domains.size)
  }

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

  /** Provides a constraint that is violated iff the current action cannot support the given Holds */
  def potentialSupportConstraint(target: Holds) : Constraint = {
    byFunction.get(target.sv.func.f) match {
      case Some(doms) =>
        val vars = (target.sv.func :: target.sv.params.toList) :+ target.value
        new PotentialSupportFeasibility(vars, doms) {
          override def toString = s"support of [$target] by [${ActionPotentialSupport.this.act}]"
        }
      case None =>
        new Contradiction
    }
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
