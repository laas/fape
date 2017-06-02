package fr.laas.fape.planning.causality.support

import fr.laas.fape.anml.model.abs.AbstractAction
import fr.laas.fape.constraints.meta.CSP
import fr.laas.fape.constraints.meta.constraints.ConjunctionConstraint
import fr.laas.fape.constraints.meta.decisions.{Decision, DecisionConstraint, DecisionOption}
import fr.laas.fape.constraints.meta.util.Assertion._
import fr.laas.fape.planning.causality.{CausalHandler, DecisionPending, SupportByActionInsertion, SupportByExistingChange}
import fr.laas.fape.planning.events.{ActionInsertion, PlanningHandler}

class SupportDecision(supportVar: SupportVar) extends Decision {

  def context(implicit csp: CSP) : CausalHandler = csp.getHandler(classOf[PlanningHandler]).getHandler(classOf[CausalHandler])

  /** Returns true is this decision is still pending. */
  override def pending(implicit csp: CSP): Boolean = {
    assert3(supportVar.dom.contains(DecisionPending) == supportVar.domain.contains(0))
    supportVar.domain.contains(0)
  }

  /** Estimate of the number of options available (typically used for variable ordering). */
  override def numOption(implicit csp: CSP): Int = {
    if(pending)
      supportVar.domain.size -1
    else
      supportVar.domain.size
  }

  /** Options to advance this decision.
    * Note that the decision can still be pending after applying one of the options.
    * A typical set of options for binary search is [var === val, var =!= val]. */
  override def options(implicit csp: CSP): Seq[DecisionOption] = {
    supportVar.dom.valuesWithIntRepresentation.toList.reverse.head match {
      case (s: SupportByActionInsertion, i) =>
        val res = new PendingSupportOption(s.a.act, supportVar)
        List(res, res.reverse(csp))
      case (s: SupportByExistingChange, i) =>
        val c = supportVar === i
        List(new DecisionConstraint(c), new DecisionConstraint(c.reverse))
      case (`DecisionPending`, i) =>
        assert(supportVar.domain.isSingleton)
        // no option when making decision, force empty domain
        List(new DecisionConstraint(supportVar =!= i))
    }
  }

  override def toString : String = s"support-decision@[${supportVar.target}"
}

class PendingSupportOption(action: AbstractAction, supportFor: SupportVar) extends DecisionOption {
  /** This method should enforce the decision option in the given CSP. */
  override def enforceIn(csp: CSP) {
    csp.addEvent(ActionInsertion(action, Some(supportFor)))

    // forbid any action insertion since we already made one
    supportFor.dom(csp).valuesWithIntRepresentation.foreach{
      case (s: SupportByActionInsertion, i) =>
        csp.post(supportFor =!= i)
      case _ =>
    }
  }

  def reverse(csp: CSP): DecisionOption = {
    // forbid this action insertion
    val constraints = supportFor.dom(csp).valuesWithIntRepresentation.toList.collect{
      case (s: SupportByActionInsertion, i) if s.a.act == action =>
        supportFor =!= i
    }
    new DecisionConstraint(new ConjunctionConstraint(constraints))
  }

  override def toString : String = s"pending-support: of '$supportFor by $action"
}
