package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;


/**
 * Evaluation function: num-actions*10 + num-consumers*3 + num-undecomposed*3
 */
public class SOCA implements PartialPlanComparator {

    public static float f(State s) {
        return s.getNumActions()*10 + s.consumers.size()*3 + s.getNumOpenLeaves()*3;
    }

    @Override
    public int compare(State state, State state2) {
        float f_state = f(state);
        float f_state2 = f(state2);

        // comparison (and not difference) is necessary since the input is a float.
        if(f_state > f_state2)
            return 1;
        else if(f_state2 > f_state)
            return -1;
        else
            return 0;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public String shortName() {
        return "soca";
    }
}
