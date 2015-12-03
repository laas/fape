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

package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.ParameterizedStateVariable;

/**
 * Unifies both state variables. This is done in unifying all their parameters.
 */
public class StateVariableBinding extends Resolver {

    public final ParameterizedStateVariable a, b;

    public StateVariableBinding(ParameterizedStateVariable a, ParameterizedStateVariable b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        st.addUnificationConstraint(a, b);
        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
