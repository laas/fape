package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

/**
 * The evaluation function is the number of roots in the task network (* 2) plus
 * the number of undecomposed actions and the number of consumers.
 *
 * Hence this strategy favours decomposition over action insertion.
 *
 * PSA+OCA in Schaddenberg's thesis, eq 4.16, p. 163
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
