package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;


/**
 * Old FAPE strategy. The cost of a partial plan is 10 times the number of actions plus
 * 3 times the number of consumers.
 *
 * The state with lowest cost is considered better.
 */
public class Actions10Consumers3 implements PartialPlanComparator {
    @Override
    public String shortName() {
        return "actions-10-cons-3";
    }

    public int eval(State s) {
        return s.taskNet.getNumActions()* 10 + s.consumers.size() * 3;
    }

    @Override
    public int compare(State state, State state2) {
        return eval(state) - eval(state2);
    }
}
