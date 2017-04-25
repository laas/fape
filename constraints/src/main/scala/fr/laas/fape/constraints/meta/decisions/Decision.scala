package fr.laas.fape.constraints.meta.decisions

import fr.laas.fape.constraints.meta.CSP

trait Decision {

  /** Returns true is this decision is still pending. */
  def pending(implicit csp: CSP): Boolean

  /** Estimate of the number of options available (typically used for variable ordering). */
  def numOption(implicit csp: CSP): Int

  /** Options to handle advance this decision.
    * Note that the decision can still be pending after applying one of the options.
    * A typical set of options for binary search is [var === val, var =!= val]. */
  def options(implicit csp: CSP): Seq[DecisionOption]
}


