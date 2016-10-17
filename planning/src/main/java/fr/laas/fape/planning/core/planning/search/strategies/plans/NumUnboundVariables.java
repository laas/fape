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

package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.states.State;

/**
 * Gives priority to state with the least number of unbound variables
 */
public class NumUnboundVariables extends PartialPlanComparator {

    @Override
    public String shortName() {
        return "unbound";
    }

    @Override
    public String reportOnState(State st) {
        return "Unbound: num-unbound: "+ st.getUnboundVariables().size();
    }

    @Override public double g(State st) { return 0; }
    @Override public double h(State st) { return st.getUnboundVariables().size(); }
    @Override public double hc(State st) { return h(st); }
}
