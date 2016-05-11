package fape.core.planning.search.strategies.plans;

import fape.core.planning.states.State;
import fape.core.planning.states.SearchNode;

import java.util.Comparator;

public abstract class PartialPlanComparator implements Comparator<SearchNode>, Heuristic {

    int id = -1;

    public abstract String shortName();

    /** Gives a human readable string of the metrics used to evaluate a state, and their values. */
    public String reportOnState(State st) {
        return shortName()+" g:"+g(st)+" h:"+h(st)+" hc:"+hc(st);
    }

    public float h(SearchNode sw) {
        if(!sw.isRecordedH(id))
            sw.setH(id, h(sw.getState()));
        return sw.getH(id);
    }
    public float g(SearchNode sw) {
        if(!sw.isRecordedG(id))
            sw.setG(id, g(sw.getState()));
        return sw.getG(id);
    }
    public float hc(SearchNode sw) {
        if(!sw.isRecordedHC(id))
            sw.setHC(id, hc(sw.getState()));
        return sw.getHC(id);
    }
    public float f(SearchNode sw) {
        return g(sw) + h(sw);
    }

    @Override
    public int compare(SearchNode s1, SearchNode s2) {
        float diff = f(s1) - f(s2);
        if(diff < 0)
            return -1;
        else if(diff > 0)
            return 1;
        else
            return 0;
    }
}
