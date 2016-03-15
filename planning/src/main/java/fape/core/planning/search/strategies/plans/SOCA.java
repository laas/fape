package fape.core.planning.search.strategies.plans;

import fape.core.planning.planner.APlanner;
import fape.core.planning.search.flaws.finders.AllThreatFinder;
import fape.core.planning.states.State;


/**
 * Evaluation function: num-actions*10 + num-consumers*3 + num-undecomposed*3
 */
public class SOCA extends PartialPlanComparator {

    private final APlanner planner;
    private final AllThreatFinder threatFinder = new AllThreatFinder();

    public SOCA(APlanner planner) { this.planner = planner; }

    @Override
    public String shortName() {
        return "soca";
    }

    @Override
    public String reportOnState(State st) {
        return "SOCA:\t g: "+g(st)+" h: "+h(st);
    }

    @Override
    public float g(State st) {
        return st.getNumActions() * 10;
    }

    @Override
    public float h(State s) {
        return threatFinder.getFlaws(s, planner).size() * 3 +
                    s.tdb.getConsumers().size()*3;
    }

    @Override
    public float hc(State st) {
        return h(st);
    }
}
