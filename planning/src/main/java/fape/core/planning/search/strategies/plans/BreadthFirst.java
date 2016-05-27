package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

/**
 * Breadth first search strategy: the least deep state is always selected first.
 */
public class BreadthFirst extends PartialPlanComparator {
    @Override
    public String shortName() {
        return "bfs";
    }

    @Override
    public String reportOnState(State st) {
        return "BFS:\t depth: "+st.getDepth();
    }

    @Override
    public double g(State st) {
        return st.getDepth();
    }

    @Override
    public double h(State st) {
        return 0;
    }

    @Override
    public double hc(State st) {
        return 0;
    }
}
