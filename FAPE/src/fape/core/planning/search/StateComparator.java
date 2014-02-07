package fape.core.planning.search;

import fape.core.planning.states.State;

import java.util.Comparator;


/**
 * Compares two states. THis is to be used for ordering states in a priority queue.
 */
public class StateComparator implements Comparator<State> {
    @Override
    public int compare(State state, State state2) {
        float f_state = state.GetCurrentCost() + state.GetGoalDistance();
        float f_state2 = state2.GetCurrentCost() + state2.GetGoalDistance();

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
}
