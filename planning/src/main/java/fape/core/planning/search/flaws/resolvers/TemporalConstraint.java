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

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof TemporalConstraint;
        TemporalConstraint o = (TemporalConstraint) e;
        if(first != o.first)
            return first.id() - o.first.id();
        if(second != o.second)
            return second.id() - o.second.id();
        if(min != o.min)
            return min - o.min;

        assert max != o.max : "Error: comparing two identical resolvers.";
        return max - o.max;
    }
}
