package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

/**
 *
 * PSA+OCA in Schaddenberg, eq 4.16, p. 163
 */
public class RootsOCA implements PartialPlanComparator {
    @Override
    public String shortName() {
        return "psaoca";
    }

    private int eval(State s) {
        return s.taskNet.getNumRoots()*2 + s.taskNet.getNumOpenLeaves() + s.consumers.size();
    }

    @Override
    public int compare(State state, State state2) {
        return eval(state) - eval(state2);
    }
}
