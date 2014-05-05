package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

import java.util.Comparator;


/**
 * Compares two states. THis is to be used for ordering states in a priority queue.
 */
public class StateComparator implements PartialPlanComparator {

    public float f(State s) {
        return s.taskNet.getNumActions()*10 + s.consumers.size()*3 + s.taskNet.getNumOpenLeaves()*3;
    }

    @Override
    public int compare(State state, State state2) {
        float f_state = f(state);
        float f_state2 = f(state2);

        // comparison (and not difference) is necessary sicne the input is a float.
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
