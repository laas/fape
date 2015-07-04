/*
 * Author:  Filip Dvorak <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvorak <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */

package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;

/**
 * Gives priority to state with the least number of unbound variables
 */
public class NumUnboundVariables implements PartialPlanComparator  {
    
    public float f(State s) {
        return s.getUnboundVariables().size();
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
        return "fex";
    }

    @Override
    public String reportOnState(State st) {
        return "Unbound: num-unbound: "+ st.getUnboundVariables().size();
    }

}
