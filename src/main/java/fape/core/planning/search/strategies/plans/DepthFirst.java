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
    public int compare(State state, State state2) {
        if (state2.depth - state.depth != 0){
            return state2.depth - state.depth;
        } else if (state2.consumers.size() - state.consumers.size() != 0){
            return state2.consumers.size() - state.consumers.size();
        } else if (state.getNumActions() - state2.getNumActions() != 0){
            return state.getNumActions() - state2.getNumActions();
        } else {
            return state2.mID - state.mID;
        }

    }
}
