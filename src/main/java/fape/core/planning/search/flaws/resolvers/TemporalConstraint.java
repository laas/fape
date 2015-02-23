/*
 * Author:  Filip Dvo��k <filip.dvorak@runbox.com>
 *
 * Copyright (c) 2013 Filip Dvo��k <filip.dvorak@runbox.com>, all rights reserved
 *
 * Publishing, providing further or using this program is prohibited
 * without previous written permission of the author. Publishing or providing
 * further the contents of this file is prohibited without previous written
 * permission of the author.
 */
package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.TPRef;

/**
 * Enforces a temporal constraints between two time points.
 */
public class TemporalConstraint extends Resolver {

    public final TPRef first, second;
    public final int min, max;

    public TemporalConstraint(TPRef first, TPRef second, int min, int max) {
        this.first = first;
        this.second = second;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean apply(State st, APlanner planner) {
        st.enforceConstraint(first, second, min, max);
        return true;
    }
}
