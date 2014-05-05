package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

public class LeastFlawRatio implements PartialPlanComparator {
    @Override
    public String shortName() {
        return "lfr";
    }

    float eval(State st) {
        return ((float) st.taskNet.getNumOpenLeaves() + st.consumers.size()) / ((float) st.taskNet.getNumActions())*100;
    }

    @Override
    public int compare(State state, State state2) {
        return (int) (eval(state) - eval(state2));
    }
}
