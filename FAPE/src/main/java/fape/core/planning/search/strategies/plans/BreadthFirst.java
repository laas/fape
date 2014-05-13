package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

/**
 * Breadth first search strategy: the least deep state is always selected first.
 */
public class BreadthFirst implements PartialPlanComparator {
    @Override
    public String shortName() {
        return "bfs";
    }

    @Override
    public int compare(State state, State state2) {
        return state.depth - state2.depth;
    }
}
