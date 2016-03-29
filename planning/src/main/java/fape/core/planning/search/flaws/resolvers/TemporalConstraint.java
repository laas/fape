package fape.core.planning.search.flaws.resolvers;

import fape.core.planning.planner.APlanner;
import fape.core.planning.states.State;
import planstack.anml.model.concrete.TPRef;

import java.util.Collections;
import java.util.List;

/**
 * Enforces a temporal constraints between two time points.
 */
public class TemporalConstraint extends Resolver {

    public final List<TPRef> firsts, seconds;
    public final int min, max;

    public TemporalConstraint(List<TPRef> firsts, List<TPRef> seconds, int min, int max) {
        this.firsts = firsts;
        this.seconds = seconds;
        this.min = min;
        this.max = max;
    }

    public TemporalConstraint(TPRef first, TPRef second, int min, int max) {
        this.firsts = Collections.singletonList(first);
        this.seconds = Collections.singletonList(second);
        this.min = min;
        this.max = max;
    }

    @Override
    public boolean apply(State st, APlanner planner, boolean isFastForwarding) {
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
