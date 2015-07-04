package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

/**
 * Depth first search strategy: the deepest plan is always selected first.
 */
public class DepthFirst implements PartialPlanComparator {
    @Override
    public String shortName() {
        return "dfs";
    }

    @Override
    public String reportOnState(State st) {
        return String.format("DFS:\t depth: %s", st.depth);
    }

    @Override
    public int compare(State state, State state2) {
        return state2.depth - state.depth;
    }
}
