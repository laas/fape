package fr.laas.fape.planning.causality

import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.{Constraint, ConstraintData, ConstraintSatisfaction, WithData}
import fr.laas.fape.constraints.meta.domains.{Domain, IntervalDomain}
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable}
import fr.laas.fape.planning.events.PlanningHandler
import fr.laas.fape.planning.structures.{Change, Holds}

import scala.collection.mutable

/** Constraint enforcing the given `holds` to be supported by a Change. */
class SupportConstraint(val holds: Holds)
  extends Constraint with WithData[SupportConstraintData] {

  /** Variable denoting which supports are possible.
    *  Each value of the domain corresponds to a change in the CausalHandler */
  val supportVar = new SupportVar

  override def onPost(implicit csp: CSP) {
    assert1(!supportVar.isInitialized)
    setData(new SupportConstraintData(csp))
    val d = data // save data field to avoid expensive accesses
    val dom = Domain(d.context.changes.indices.toSet)
    for(i <- dom.values) {
      d.put(i, supportConstraintForChange(d.context.changes(i)))
      csp.watchSubConstraint(d.constraintOf(i), this)
    }
    csp.updateDomain(supportVar, dom)
    assert1(supportVar.isInitialized)
    super.onPost
  }

  override def variables(implicit csp: CSP): Set[IVar] = Set(supportVar)

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    if(supportVar.domain.isSingleton && data.constraintOf(supportVar.domain.values.head).isSatisfied)
      ConstraintSatisfaction.SATISFIED
    else if(supportVar.domain.isEmpty)
      ConstraintSatisfaction.VIOLATED
    else
      ConstraintSatisfaction.UNDEFINED
  }

  override protected def _propagate(event: Event)(implicit csp: CSP) {
    event match {
      case NewConstraint(c) =>
        assert1(c == this) // nothing to do, everything initialized in onPost
      case DomainReduced(`supportVar`) =>
        val d = data
        if(supportVar.domain.isSingleton) {
          val c = d.constraintOf(supportVar.domain.values.head)
          csp.postSubConstraint(c, this)
        }
      case WatchedSatisfied(c) =>
        val d = data
        val i = d.indexOf(c)
        assert1(supportVar.domain.contains(i), "A support is entailed but not in the support variable domain")
        csp.updateDomain(supportVar, Domain(Set(i)))
      case WatchedViolated(c) =>
        val d = data
        val i = d.indexOf(c)
        csp.updateDomain(supportVar, supportVar.domain - i)
    }
  }

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint = ???

  private def supportConstraintForChange(c: Change) : Constraint =
    if(holds.precedingChange)
      holds.sv === c.sv &&
        holds.value === c.value &&
        holds.persists.start >= c.persists.start &&
        holds.persists.end === c.persists.end
    else
      holds.sv === c.sv &&
        holds.value === c.value &&
        holds.persists.start >= c.persists.start &&
        holds.persists.end <= c.persists.end
}

class SupportConstraintData(_csp: CSP, base: Option[SupportConstraintData] = None) extends ConstraintData {

  val context : CausalHandler = _csp.getHandler(classOf[PlanningHandler]).getHandler(classOf[CausalHandler])

  private val constraintsByChangeIndex: scala.collection.mutable.Map[Int, Constraint] = base match {
    case Some(prev) => prev.constraintsByChangeIndex.clone()
    case None => mutable.Map()
  }
  private val changeIndexByConstraint: scala.collection.mutable.Map[Constraint, Int] = base match {
    case Some(prev) => prev.changeIndexByConstraint.clone()
    case None => mutable.Map()
  }

  def put(changeIndex: Int, subConstraint: Constraint) {
    assert1(!constraintsByChangeIndex.contains(changeIndex))
    constraintsByChangeIndex.put(changeIndex, subConstraint)
    changeIndexByConstraint.put(subConstraint, changeIndex)
  }

  def constraintOf(changeIndex: Int) : Constraint = constraintsByChangeIndex(changeIndex)

  def indexOf(constraint: Constraint) : Int = changeIndexByConstraint(constraint)

  def clone(implicit context: CSP) = new SupportConstraintData(context, Some(this))
}


class SupportVar extends IntVariable(new IntervalDomain(0, Integer.MAX_VALUE/2)) {

  def isInitialized(implicit csp: CSP) = !domain.contains(Integer.MAX_VALUE/2)

  override def toString = "support-var@"+hashCode()
}
