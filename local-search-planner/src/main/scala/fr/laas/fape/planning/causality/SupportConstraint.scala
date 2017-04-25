package fr.laas.fape.planning.causality

import fr.laas.fape.anml.model.concrete.{Action, Factory}
import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints._
import fr.laas.fape.constraints.meta.domains.{Domain, IntervalDomain}
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.types.dynamics.{DynTypedVariable, DynamicType}
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.meta.variables.{IVar, IntVariable}
import fr.laas.fape.planning.events.{ActionInsertion, PlanningHandler}
import fr.laas.fape.planning.structures.{Change, Holds}

import scala.collection.mutable

/** Constraint enforcing the given `holds` to be supported by a Change. */
class SupportConstraint(t: DynamicType[SupportOption], val holds: Holds)
  extends Constraint with WithData[SupportConstraintData] {

  /** Variable denoting which supports are possible.
    *  Each value of the domain corresponds to a change in the CausalHandler */
  val supportVar = new SupportVar(t)

  override def onPost(implicit csp: CSP) {
    setData(new SupportConstraintData(csp))
    processDomain
    super.onPost
  }

  override def variables(implicit csp: CSP): Set[IVar] = Set(supportVar)

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    if(supportVar.domain.isSingleton) {
      supportVar.dom.values.head match {
        case DecisionPending => ConstraintSatisfaction.VIOLATED
        case SupportByExistingChange(c) if data.constraintOf(supportVar.domain.values.head).isSatisfied =>
            ConstraintSatisfaction.SATISFIED
        case _ => ConstraintSatisfaction.UNDEFINED
      }
    } else if(supportVar.domain.isEmpty)
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
          val domainValue = supportVar.domain.values.head
          val value = supportVar.dom.values.head
          value match {
            case DecisionPending =>
              // violated, no decision made and no option left
              throw new InconsistentBindingConstraintNetwork()
            case SupportByExistingChange(change) =>
              val c = d.constraintOf(domainValue)
              csp.postSubConstraint(c, this)
            case SupportByActionInsertion(aps) =>
              println("ACTION SUPPORT NEEDED!!!!! by " + aps.act)
              csp.addEvent(ActionInsertion(aps.act, Some(supportVar)))
          }
        }
      case DomainExtended(`supportVar`) =>
        processDomain
      case WatchedSatisfied(c) =>
        val d = data
        val i = d.indexOf(c)
        val support = t.static.intToInstance(i)
        support match {
          case SupportByExistingChange(change) =>
            // enforce, the constraint is exclusive of any other support
            assert1(supportVar.domain.contains(i), "A support is entailed but not in the support variable domain")
            csp.updateDomain(supportVar, Domain(Set(i)))
          case SupportByActionInsertion(aps) =>
            // ignore, even if it is satisfied it does not mean we have no other options
          case DecisionPending => throw new RuntimeException("Not expected")
        }
      case WatchedViolated(c) =>
        val d = data
        val i = d.indexOf(c)
        csp.updateDomain(supportVar, supportVar.domain - i)
    }
  }

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint = ???

  private def processDomain(implicit csp: CSP) {
    val d = data // save data field to avoid expensive accesses
    val dom = supportVar.domain
    val mapping = t.static
    for(i <- dom.values if !d.hasConstraintFor(i)) {
      mapping.intToInstance(i) match {
        case DecisionPending =>
        case SupportByExistingChange(change) =>
          val c = supportConstraintForChange(change)
          d.put(i, c)
          csp.watchSubConstraint(c, this)
        case SupportByActionInsertion(aps) =>
          val c = new Tautology  // TODO: useful constraint
          d.put(i, c)
          csp.watchSubConstraint(c, this)
      }
    }
  }

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

class SupportByAction(act: Action, supportVar: SupportVar) extends Constraint {
  override def variables(implicit csp: CSP): Set[IVar] = Set(supportVar)

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    if(supportVar.dom.values.exists(!_.isInstanceOf[SupportByExistingChange]))
      ConstraintSatisfaction.UNDEFINED
    else if(supportVar.dom.values.collect{ case x: SupportByExistingChange => x.c}.forall(_.ref.container == act.container))
      ConstraintSatisfaction.SATISFIED
    else
      ConstraintSatisfaction.UNDEFINED
  }

  override protected def _propagate(event: Event)(implicit csp: CSP): Unit = {
    if(supportVar.domain.isEmpty)
      throw new InconsistentBindingConstraintNetwork()
    for((v, i) <- supportVar.dom.valuesWithIntRepresentation) {
      v match {
        case SupportByExistingChange(change) if change.ref.container == act.chronicle =>
        case _ => csp.post(supportVar =!= i)
      }
    }
  }

  /** Returns the invert of this constraint (e.g. === for an =!= constraint) */
  override def reverse: Constraint = ???
}


class SupportVar(t: DynamicType[SupportOption]) extends DynTypedVariable[SupportOption](t) {


  override def toString = "support-var@"+hashCode()
}
