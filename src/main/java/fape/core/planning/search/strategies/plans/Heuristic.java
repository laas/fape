package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

public interface Heuristic {

    /** Current cost of the partial plan */
    public float g(State st);

    /** Estimation of the cost to go */
    public float h(State st);

    /** Estimation of the distance to go (in terms of search) */
    public float hc(State st);
}
