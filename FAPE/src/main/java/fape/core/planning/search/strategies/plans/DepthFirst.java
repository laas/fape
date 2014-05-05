package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

public class DepthFirst implements PartialPlanComparator {
    @Override
    public String shortName() {
        return "dfs";
    }

    @Override
    public int compare(State state, State state2) {
        return state2.depth - state.depth;
    }
}
