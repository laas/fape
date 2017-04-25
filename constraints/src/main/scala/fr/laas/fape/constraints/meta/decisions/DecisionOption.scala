package fr.laas.fape.constraints.meta.decisions

import fr.laas.fape.constraints.meta.CSP

trait DecisionOption {

  /** This method should enforce the decision option in the given CSP. */
  def enforceIn(csp: CSP)

}
