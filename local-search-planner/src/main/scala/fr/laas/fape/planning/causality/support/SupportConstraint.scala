package fr.laas.fape.planning.causality.support

import fr.laas.fape.constraints.bindings.InconsistentBindingConstraintNetwork
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints._
import fr.laas.fape.constraints.meta.domains.Domain
import fr.laas.fape.constraints.meta.events._
import fr.laas.fape.constraints.meta.types.dynamics.DynamicType
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.constraints.meta.variables.IVar
import fr.laas.fape.planning.causality.{DecisionPending, SupportByActionInsertion, SupportByExistingChange, SupportOption}
import fr.laas.fape.planning.structures.{Change, Holds}

/** Constraint enforcing the given `holds` to be supported by a Change. */
class SupportConstraint(t: DynamicType[SupportOption], val holds: Holds)
  extends Constraint with WithData[SupportConstraintData] {

  /** Variable denoting which supports are possible.
    *  Each value of the domain corresponds to a change in the CausalHandler */
  val supportVar = new SupportVar(t, holds)
  private val decision = new SupportDecision(supportVar)

  override def onPost(implicit csp: CSP) {
    csp.decisions.add(decision)
    setData(new SupportConstraintData(csp))
    processDomain
    super.onPost
  }

  override def variables(implicit csp: CSP): Set[IVar] = Set(supportVar)

  override def satisfaction(implicit csp: CSP): Satisfaction = {
    if(supportVar.domain.isSingleton) {
      supportVar.dom.values.head match {
        case SupportByExistingChange(c) if data.constraintOf(supportVar.domain.values.head).isSatisfied =>
          assert3(!(holds.sv === c.sv).isViolated)
          assert3(!(holds.value === c.value).isViolated)
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
            case SupportByExistingChange(change) =>
              val c = d.constraintOf(domainValue)
              csp.postSubConstraint(c, this)
            case SupportByActionInsertion(aps) =>
              throw new RuntimeException("Not expected")
          }
        } else if(supportVar.domain.isEmpty) {
          throw new InconsistentBindingConstraintNetwork()
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
            csp.updateDomain(supportVar, Domain(i))
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
          val c = aps.potentialSupportConstraint(holds)
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







