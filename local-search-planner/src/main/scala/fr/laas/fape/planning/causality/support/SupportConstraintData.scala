package fr.laas.fape.planning.causality.support

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintData}
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.planning.causality.CausalHandler
import fr.laas.fape.planning.events.PlanningHandler

import scala.collection.mutable

/** Data fields to be accessed by a SupportConstraint */
class SupportConstraintData(_csp: CSP, base: Option[SupportConstraintData] = None) extends ConstraintData {

  val context : CausalHandler = _csp.getHandler(classOf[PlanningHandler]).getHandler(classOf[CausalHandler])

  private val constraintsByDomainValue: scala.collection.mutable.Map[Int, Constraint] = base match {
    case Some(prev) => prev.constraintsByDomainValue.clone()
    case None => mutable.Map()
  }
  private val domainValueByConstraint: scala.collection.mutable.Map[Constraint, Int] = base match {
    case Some(prev) => prev.domainValueByConstraint.clone()
    case None => mutable.Map()
  }

  def put(domainValue: Int, subConstraint: Constraint) {
    assert1(!constraintsByDomainValue.contains(domainValue))
    constraintsByDomainValue.put(domainValue, subConstraint)
    domainValueByConstraint.put(subConstraint, domainValue)
  }

  def hasConstraintFor(domainValue: Int) = constraintsByDomainValue.contains(domainValue)

  def constraintOf(domainValue: Int) : Constraint = constraintsByDomainValue(domainValue)

  def indexOf(constraint: Constraint) : Int = domainValueByConstraint(constraint)

  def clone(implicit context: CSP) = new SupportConstraintData(context, Some(this))
}
