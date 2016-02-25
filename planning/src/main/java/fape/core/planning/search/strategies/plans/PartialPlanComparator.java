package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;
import fape.core.planning.states.StateWrapper;

import java.util.Comparator;

public abstract class PartialPlanComparator implements Comparator<StateWrapper>, Heuristic {

    int id;

    abstract String shortName();

    /** Gives a human readable string of the metrics used to evaluate a state, and their values. */
    abstract String reportOnState(State st);

    final public float h(StateWrapper sw) {
        if(!sw.isRecordedH(id))
            sw.setH(id, h(sw.getState()));
        return sw.getH(id);
    }
    final public float g(StateWrapper sw) {
        if(!sw.isRecordedG(id))
            sw.setG(id, g(sw.getState()));
        return sw.getG(id);
    }
    final public float hc(StateWrapper sw) {
        if(!sw.isRecordedHC(id))
            sw.setHC(id, hc(sw.getState()));
        return sw.getHC(id);
    }
    final public float f(StateWrapper sw) {
        return g(sw) + h(sw);
    }

    @Override
    public int compare(StateWrapper s1, StateWrapper s2) {
        float diff = f(s1) - f(s2);
        if(diff < 0)
            return -1;
        else if(diff > 0)
            return 1;
        else
            return 0;
    }
}
