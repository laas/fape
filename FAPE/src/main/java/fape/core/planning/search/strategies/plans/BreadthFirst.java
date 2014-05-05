package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

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
