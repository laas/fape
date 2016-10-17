package fr.laas.fape.planning.core.planning.search.flaws.resolvers;


import fr.laas.fape.anml.model.concrete.TPRef;
import fr.laas.fape.planning.core.planning.planner.Planner;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.List;

/**
 * Enforces a temporal constraints between two time points.
 */
public class TemporalConstraint implements Resolver {

    private final List<TPRef> firsts, seconds;
    private final int min, max;

    public TemporalConstraint(List<TPRef> firsts, List<TPRef> seconds, int min, int max) {
        this.firsts = firsts;
        this.seconds = seconds;
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean apply(State st, Planner planner, boolean isFastForwarding) {
        for(TPRef first : firsts)
            for(TPRef second : seconds)
                st.enforceConstraint(first, second, min, max);
        return true;
    }

    @Override
    public int compareWithSameClass(Resolver e) {
        assert e instanceof TemporalConstraint;
        TemporalConstraint o = (TemporalConstraint) e;
        for(TPRef first : firsts)
            for(TPRef ofirst : o.firsts)
                if(first != ofirst)
                    return first.id() - ofirst.id();
        for(TPRef second : seconds)
            for(TPRef osecond : o.seconds)
                if(second != osecond)
                    return second.id() - osecond.id();
        if(min != o.min)
            return min - o.min;

        assert max != o.max : "Error: comparing two identical resolvers.";
        return max - o.max;
    }
}
