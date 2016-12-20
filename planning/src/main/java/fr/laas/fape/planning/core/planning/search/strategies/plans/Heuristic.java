package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.PartialPlan;

public interface Heuristic {

    /** Current cost of the partial plan */
    double g(PartialPlan plan);

    /** Estimation of the cost to go */
    double h(PartialPlan plan);

    /** Estimation of the distance to go (in terms of search) */
    double hc(PartialPlan plan);
}
