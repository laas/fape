package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

public interface Heuristic {

    /** Current cost of the partial plan */
    float g(State st);

    /** Estimation of the cost to go */
    float h(State st);

    /** Estimation of the distance to go (in terms of search) */
    float hc(State st);
}
