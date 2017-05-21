package fr.laas.fape.constraints.meta

import fr.laas.fape.constraints.meta.search.heuristics.{DFSHeuristic, Heuristic}

class Configuration(
                     val enforceTpAfterStart: Boolean = true, // if true, any timepoint added to the STN will enforced to be greater or equal than csp.temporalOrigin
                     val initialDepth: Int = 0,
                     val initialHeuristicBuilder: CSP => Heuristic = (csp: CSP) => new DFSHeuristic(csp)
                   )
