package fape.core.planning.search.strategies.flaws;

import fape.core.planning.planner.Planner;
import fape.core.planning.search.flaws.flaws.Flaw;
import fape.core.planning.search.flaws.flaws.Threat;
import fape.core.planning.search.flaws.flaws.UnmotivatedAction;
import fape.core.planning.search.flaws.flaws.UnrefinedTask;
import fape.core.planning.states.State;

public class HierarchicalFirstComp implements FlawComparator {

    public final State st;
    public final Planner planner;
    public final boolean threatsFirst;

    public HierarchicalFirstComp(State st, Planner planner) {
        this.st = st;
        this.planner = planner;
        threatsFirst = st.pb.allActionsAreMotivated();
    }

    @Override
    public String shortName() {
        return "hier";
    }

    private double priority(Flaw flaw) {
        if(threatsFirst && flaw instanceof Threat)
            return 0.;
        else if(flaw instanceof UnrefinedTask)
            return 1. + ((double) st.getEarliestStartTime(((UnrefinedTask) flaw).task.start())) / Integer.MAX_VALUE; // ((double) st.getEarliestStartTime(st.pb.end()) + 1);
        else if(flaw instanceof UnmotivatedAction)
            return 4.;
        else
            return 5.;
    }


    @Override
    public int compare(Flaw o1, Flaw o2) {
        return (int) Math.signum(priority(o1) - priority(o2));
    }
}
