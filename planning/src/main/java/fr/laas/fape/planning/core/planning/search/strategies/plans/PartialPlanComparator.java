package fr.laas.fape.planning.core.planning.search.strategies.plans;

import fr.laas.fape.planning.core.planning.planner.PlanningOptions;
import fr.laas.fape.planning.core.planning.states.SearchNode;
import fr.laas.fape.planning.core.planning.states.State;

import java.util.Comparator;

public abstract class PartialPlanComparator implements Heuristic {

    public abstract String shortName();

    /** Gives a human readable string of the metrics used to evaluate a state, and their values. */
    public String reportOnState(State st) {
        return shortName()+" g:"+g(st)+" h:"+h(st)+" hc:"+hc(st);
    }

    public final double h(SearchNode sw) {
        if(!sw.isRecordedH())
            sw.setH(h(sw.getState()));
        return sw.getH();
    }
    public final double g(SearchNode sw) {
        if(!sw.isRecordedG())
            sw.setG(g(sw.getState()));
        return sw.getG();
    }
    public final double hc(SearchNode sw) {
        if(!sw.isRecordedHC())
            sw.setHC(hc(sw.getState()));
        return sw.getHC();
    }

    public final Comparator<SearchNode> comparator(PlanningOptions options) {
        return new Comparator<SearchNode>() {
            private double f(SearchNode sn) { return g(sn) + options.heuristicWeight * h(sn); }
            @Override
            public int compare(SearchNode st1, SearchNode st2) {
                int ret = (int) Math.signum(f(st1) - f(st2));
                if(ret != 0)
                    return ret;
                else
                    return st1.getID() - st2.getID();
            }
        };
    }
}
